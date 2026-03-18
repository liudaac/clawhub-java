import { test, expect } from '@playwright/test'

test.describe('Skills Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/skills')
  })

  test('should display skills list', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Skills' })).toBeVisible()
    await expect(page.locator('[data-testid="skill-card"]').first()).toBeVisible()
  })

  test('should filter by sort order', async ({ page }) => {
    await page.getByRole('button', { name: 'Most Downloaded' }).click()
    await expect(page).toHaveURL(/.*sort=downloads.*/)
    
    await page.getByRole('button', { name: 'Most Starred' }).click()
    await expect(page).toHaveURL(/.*sort=stars.*/)
  })

  test('should navigate to skill detail', async ({ page }) => {
    const firstSkill = page.locator('[data-testid="skill-card"]').first()
    await firstSkill.click()
    
    await expect(page).toHaveURL(/.*skills\/.+/)
    await expect(page.getByRole('heading').first()).toBeVisible()
  })

  test('should have pagination', async ({ page }) => {
    await expect(page.getByRole('button', { name: 'Next' })).toBeVisible()
    
    // If there's a next page
    const nextButton = page.getByRole('button', { name: 'Next' })
    if (await nextButton.isEnabled()) {
      await nextButton.click()
      await expect(page).toHaveURL(/.*page=1.*/)
    }
  })
})

test.describe('Skill Detail Page', () => {
  test('should display skill information', async ({ page }) => {
    await page.goto('/skills/test-skill')
    
    await expect(page.getByRole('heading').first()).toBeVisible()
    await expect(page.getByText('downloads')).toBeVisible()
    await expect(page.getByText('stars')).toBeVisible()
  })

  test('should have install button', async ({ page }) => {
    await page.goto('/skills/test-skill')
    
    await expect(page.getByRole('button', { name: /install/i })).toBeVisible()
  })

  test('should navigate back to skills list', async ({ page }) => {
    await page.goto('/skills/test-skill')
    
    await page.getByRole('button', { name: 'Back to Skills' }).click()
    await expect(page).toHaveURL('/skills')
  })
})
