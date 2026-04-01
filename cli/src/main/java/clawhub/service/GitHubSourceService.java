package clawhub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * GitHub源服务
 * 支持从GitHub仓库直接获取源码
 *
 * 对应原版: packages/clawdhub/src/cli/commands/github.ts
 */
public class GitHubSourceService {

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
        "(?:https?://)?(?:www\\.)?github\\.com/([^/]+)/([^/]+)(?:/(?:tree|blob)/([^/]+)(?:/(.+))?)?"
    );

    private static final Pattern SHORT_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9_-]+)/([a-zA-Z0-9._-]+)(?:@([a-zA-Z0-9._-]+))?$"
    );

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 解析源输入，支持多种形式：
     * - owner/repo
     * - owner/repo@ref
     * - https://github.com/owner/repo
     * - https://github.com/owner/repo/tree/main/subdir
     *
     * @param input 源输入
     * @return GitHub源信息
     */
    public GitHubSource resolveSourceInput(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Source input cannot be empty");
        }

        // 尝试匹配完整URL
        Matcher urlMatcher = GITHUB_URL_PATTERN.matcher(input);
        if (urlMatcher.matches()) {
            return new GitHubSource(
                urlMatcher.group(1),  // owner
                urlMatcher.group(2),  // repo
                urlMatcher.group(3),  // ref
                urlMatcher.group(4)   // path
            );
        }

        // 尝试匹配短格式
        Matcher shortMatcher = SHORT_PATTERN.matcher(input);
        if (shortMatcher.matches()) {
            return new GitHubSource(
                shortMatcher.group(1),  // owner
                shortMatcher.group(2),  // repo
                shortMatcher.group(3),  // ref
                null                    // path
            );
        }

        throw new IllegalArgumentException("Invalid GitHub source format: " + input);
    }

    /**
     * 从GitHub获取源码到临时目录
     *
     * @param source GitHub源信息
     * @return 获取的源码信息
     * @throws IOException 如果下载或解压失败
     */
    public FetchedSource fetchGitHubSource(GitHubSource source) throws IOException {
        Path tempDir = Files.createTempDirectory("clawhub-github-" + source.repo() + "-");

        String ref = source.ref() != null ? source.ref() : "HEAD";
        String downloadUrl = String.format(
            "https://github.com/%s/%s/archive/%s.zip",
            source.owner(), source.repo(), ref
        );

        Path zipFile = tempDir.resolve("source.zip");

        try {
            // 下载
            System.out.println("Downloading from GitHub: " + downloadUrl);
            downloadFile(downloadUrl, zipFile);

            // 解压
            System.out.println("Extracting...");
            Path extractedDir = unzip(zipFile, tempDir);

            // 如果指定了子路径，返回子目录
            Path sourceDir = extractedDir;
            if (source.path() != null && !source.path().isEmpty()) {
                sourceDir = extractedDir.resolve(source.path());
                if (!Files.exists(sourceDir)) {
                    throw new IOException("Specified path not found in repository: " + source.path());
                }
            }

            // 解析本地git信息
            Optional<GitInfo> gitInfo = resolveLocalGitInfo(sourceDir);
            if (gitInfo.isPresent()) {
                System.out.println("Git commit: " + gitInfo.get().commit().substring(0, 8));
            }

            return new FetchedSource(sourceDir, () -> deleteDirectory(tempDir));

        } catch (Exception e) {
            // 清理临时目录
            deleteDirectory(tempDir);
            throw e;
        }
    }

    /**
     * 标准化GitHub仓库地址
     *
     * @param repo 仓库地址
     * @return 标准化的 owner/repo 格式
     */
    public String normalizeGitHubRepo(String repo) {
        if (repo == null) return null;

        // 移除协议前缀
        String normalized = repo.replaceAll("^(?:https?://)?(?:www\\.)?github\\.com/", "");

        // 验证格式
        if (!normalized.matches("^[a-zA-Z0-9_-]+/[a-zA-Z0-9._-]+$")) {
            throw new IllegalArgumentException("Invalid GitHub repo format: " + repo);
        }

        return normalized;
    }

    /**
     * 解析本地git信息
     *
     * @param directory 源码目录
     * @return Git信息
     */
    public Optional<GitInfo> resolveLocalGitInfo(Path directory) {
        Path gitDir = directory.resolve(".git");
        if (!Files.exists(gitDir)) {
            return Optional.empty();
        }

        try {
            // 获取当前commit
            String commit = runGitCommand(directory, "rev-parse", "HEAD").trim();

            // 获取当前分支
            String ref = runGitCommand(directory, "rev-parse", "--abbrev-ref", "HEAD").trim();

            // 获取远程origin
            String remote = runGitCommand(directory, "remote", "get-url", "origin").trim();

            return Optional.of(new GitInfo(commit, ref, remote));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 下载文件
     */
    private void downloadFile(String url, Path destination) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "ClawHub-CLI/1.0");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to download: HTTP " + responseCode);
        }

        try (InputStream in = conn.getInputStream();
             OutputStream out = Files.newOutputStream(destination)) {
            in.transferTo(out);
        }
    }

    /**
     * 解压ZIP文件
     */
    private Path unzip(Path zipFile, Path destination) throws IOException {
        Path extractedDir = null;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = destination.resolve(entry.getName());

                // 安全检查：防止zip slip
                if (!entryPath.normalize().startsWith(destination.normalize())) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    if (extractedDir == null) {
                        extractedDir = entryPath;
                    }
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        if (extractedDir == null) {
            throw new IOException("No files extracted from ZIP");
        }

        return extractedDir;
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;

        Files.walk(dir)
            .sorted((a, b) -> -a.compareTo(b)) // 先删除子目录
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // ignore
                }
            });
    }

    /**
     * 运行git命令
     */
    private String runGitCommand(Path directory, String... args) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git");
        pb.directory(directory.toFile());
        for (String arg : args) {
            pb.command().add(arg);
        }

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Git command failed with exit code: " + exitCode);
        }

        return output;
    }

    /**
     * GitHub源信息
     */
    public record GitHubSource(String owner, String repo, String ref, String path) {
        /**
         * 获取完整仓库名
         */
        public String fullName() {
            return owner + "/" + repo;
        }

        /**
         * 获取GitHub URL
         */
        public String toUrl() {
            return String.format("https://github.com/%s/%s", owner, repo);
        }
    }

    /**
     * 获取的源码信息
     */
    public record FetchedSource(Path directory, Cleanup cleanup) {
        /**
         * 清理回调接口
         */
        public interface Cleanup {
            void run() throws IOException;
        }
    }

    /**
     * Git信息
     */
    public record GitInfo(String commit, String ref, String remote) {
        /**
         * 获取短commit hash
         */
        public String shortCommit() {
            return commit != null && commit.length() >= 8 ? commit.substring(0, 8) : commit;
        }
    }
}