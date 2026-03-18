import { test, expect } from '@playwright/test'

test.describe('Home Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
  })

  test('should display hero section', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Discover & Share Skills' })).toBeVisible()
    await expect(page.getByText('ClawHub is a registry for AI skills and agent souls')).toBeVisible()
  })

  test('should have navigation links', async ({ page }) => {
    await expect(page.getByRole('link', { name: 'Browse Skills' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Publish Skill' })).toBeVisible()
  })

  test('should navigate to skills page', async ({ page }) => {
    await page.getByRole('link', { name: 'Browse Skills' }).click()
    await expect(page).toHaveURL('/skills')
    await expect(page.getByRole('heading', { name: 'Skills' })).toBeVisible()
  })

  test('should display trending skills section', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Trending Skills' })).toBeVisible()
  })

  test('should display new skills section', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'New Skills' })).toBeVisible()
  })

  test('should have search functionality', async ({ page }) => {
    const searchInput = page.getByPlaceholder('Search skills...')
    await expect(searchInput).toBeVisible()
    
    await searchInput.fill('test')
    await searchInput.press('Enter')
    
    await expect(page).toHaveURL(/.*search.*/)
  })
})
