import { test, expect } from '@playwright/test'

test.describe('Authentication', () => {
  test('should show login button when not authenticated', async ({ page }) => {
    await page.goto('/')
    
    await expect(page.getByRole('button', { name: 'Login with GitHub' })).toBeVisible()
  })

  test('should redirect to login when accessing protected page', async ({ page }) => {
    await page.goto('/upload')
    
    // Should show login prompt or redirect
    await expect(page.getByText(/login|sign in/i)).toBeVisible()
  })

  test('should have GitHub OAuth link', async ({ page }) => {
    await page.goto('/')
    
    const loginButton = page.getByRole('button', { name: 'Login with GitHub' })
    await loginButton.click()
    
    // Should redirect to GitHub OAuth
    await expect(page).toHaveURL(/.*github.com.*oauth.*/)
  })
})

test.describe('Authenticated User', () => {
  // These tests would require mocking authentication
  // or using a test user account
  
  test('should show user menu when authenticated', async ({ page }) => {
    // This would require setting up authentication state
    // await page.goto('/')
    // await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible()
  })

  test('should allow creating skill when authenticated', async ({ page }) => {
    // This would require authentication setup
    // await page.goto('/upload')
    // await expect(page.getByRole('heading', { name: 'Upload New Skill' })).toBeVisible()
  })
})
