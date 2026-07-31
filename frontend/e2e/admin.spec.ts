import { expect, test } from '@playwright/test';
import {
  clickText,
  dataRows,
  deleteRowWithText,
  expectPlaceholderValue,
  fillByPlaceholder,
  login,
  modalVisible,
  pickOption,
} from './helpers';

test.describe('Admin CRUD trên mock API', () => {
  test('login và điều hướng vào admin dashboard', async ({ page }) => {
    await login(page);
    await page.goto('/', { waitUntil: 'networkidle' });
    await page.getByRole('button', { name: /Mở menu người dùng/ }).click();
    await page.getByRole('button', { name: /Quản trị viên/ }).click();
    await expect(page).toHaveURL(/\/admin\/dashboard/);
  });

  test('movie: tạo, sửa, xóa', async ({ page }) => {
    await login(page);
    await page.goto('/admin/movie', { waitUntil: 'networkidle' });
    const before = await dataRows(page);

    await page.locator('button[nzType="primary"]').click();
    await page.locator('.ant-modal input').first().waitFor();
    await fillByPlaceholder(page, 'Tên phim', 'Phim E2E Test');
    await fillByPlaceholder(page, 'image.tmdb', 'https://image.tmdb.org/t/p/w300/x.jpg', 1);
    await page.locator('.ant-modal .ant-picker input').first().click();
    await page.locator('.ant-picker-cell-today').click();
    await fillByPlaceholder(page, 'ví dụ', '120');
    await fillByPlaceholder(page, 'Tiêu đề gốc', 'E2E Original');
    await page.locator('.ant-modal textarea').fill('E2E overview');
    const selects = page.locator('.ant-modal .ant-select');
    await selects.nth(2).click();
    await page.locator('.ant-modal .ant-select-selection-search-input').last().fill('Action');
    await page.keyboard.press('Enter');
    await selects.nth(0).click();
    await pickOption(page);
    await selects.nth(1).click();
    await pickOption(page);

    expect(await clickText(page, '.ant-modal button', 'Lưu')).toBe(true);
    await expect.poll(() => modalVisible(page)).toBe(false);
    await expect.poll(() => dataRows(page)).toBe(before + 1);

    const row = page.locator('.ant-table-row').filter({ hasText: 'Phim E2E Test' }).first();
    await row.getByRole('button', { name: 'Sửa' }).click();
    await page.locator('.ant-modal input').first().waitFor();
    await expectPlaceholderValue(page, 'Tên phim', 'Phim E2E Test');
    await fillByPlaceholder(page, 'Tên phim', 'Phim E2E Edit');
    await clickText(page, '.ant-modal button', 'Lưu');
    await expect
      .poll(() => page.locator('.ant-table-row').filter({ hasText: 'Phim E2E Edit' }).count())
      .toBeGreaterThan(0);

    expect(await deleteRowWithText(page, 'Phim E2E Edit', before)).toBe(true);
  });

  test('theater: tạo, sửa, xóa', async ({ page }) => {
    await login(page);
    await page.goto('/admin/theater', { waitUntil: 'networkidle' });
    const before = await dataRows(page);

    await page.locator('button[nzType="primary"]').click();
    await page.locator('.ant-modal input').first().waitFor();
    await fillByPlaceholder(page, 'Tên rạp', 'Rạp E2E Test');
    await fillByPlaceholder(page, 'Địa điểm', '123 E2E St');
    await page.locator('.ant-modal .ant-select').click();
    await pickOption(page, 1);

    expect(await clickText(page, '.ant-modal button', 'Lưu')).toBe(true);
    await expect.poll(() => modalVisible(page)).toBe(false);
    await expect.poll(() => dataRows(page)).toBe(before + 1);

    const row = page.locator('.ant-table-row').filter({ hasText: 'Rạp E2E Test' }).first();
    await row.getByRole('button', { name: 'Sửa' }).click();
    await page.locator('.ant-modal input').first().waitFor();
    await expectPlaceholderValue(page, 'Tên rạp', 'Rạp E2E Test');
    await fillByPlaceholder(page, 'Tên rạp', 'Rạp E2E Edit');
    await clickText(page, '.ant-modal button', 'Lưu');
    await expect
      .poll(() => page.locator('.ant-table-row').filter({ hasText: 'Rạp E2E Edit' }).count())
      .toBeGreaterThan(0);

    expect(await deleteRowWithText(page, 'Rạp E2E Edit', before)).toBe(true);
  });

  test('showtime: tạo suất chiếu', async ({ page }) => {
    await login(page);
    await page.goto('/admin/showtime', { waitUntil: 'networkidle' });
    const before = await dataRows(page);

    await page.locator('button[nzType="primary"]').click();
    await page.locator('.ant-modal input').first().waitFor();

    await page.locator('.ant-modal input[placeholder*="Tìm"]').click();
    await page.keyboard.type('CGV');
    const movieResult = page.locator('.ant-modal li[role="tab"]').first();
    await expect(movieResult).toBeVisible();
    await movieResult.click();

    await page.locator('.ant-modal .ant-select').nth(0).click();
    await pickOption(page);

    await page.locator('.ant-modal .ant-picker').nth(0).locator('input').click();
    await page.locator('.ant-picker-cell-today').click();

    const timeInput = page.locator('.ant-modal .ant-picker').nth(1).locator('input');
    await timeInput.click();
    await timeInput.fill('19:30:00');
    await timeInput.press('Enter');

    await page.locator('.ant-modal .ant-select').nth(1).click();
    await pickOption(page);
    await page.locator('.ant-modal .ant-select').nth(2).click();
    await pickOption(page);

    expect(await clickText(page, '.ant-modal button', 'Lưu')).toBe(true);
    await expect.poll(() => modalVisible(page)).toBe(false);
    await expect.poll(() => dataRows(page)).toBe(before + 1);
  });

  test('voucher: tạo, sửa, xóa', async ({ page }) => {
    await login(page);
    await page.goto('/admin/voucher', { waitUntil: 'networkidle' });
    const before = await dataRows(page);

    await page.locator('button[nzType="primary"]').click();
    await page.locator('.ant-modal input').first().waitFor();
    const inputs = page.locator('.ant-modal input');
    await inputs.nth(0).fill('Voucher E2E Test');
    await inputs.nth(1).fill('E2E2026');
    await inputs.nth(2).fill('2026-07-31');
    await inputs.nth(2).press('Enter');
    await inputs.nth(3).fill('2026-08-31');
    await inputs.nth(3).press('Enter');

    expect(await clickText(page, '.ant-modal button', 'Lưu thay đổi')).toBe(true);
    await expect.poll(() => modalVisible(page)).toBe(false);
    await expect.poll(() => dataRows(page)).toBe(before + 1);

    const row = page.locator('.ant-table-row').filter({ hasText: 'Voucher E2E Test' }).first();
    await row.getByRole('button', { name: 'Sửa' }).click();
    await page.locator('.ant-modal input').first().waitFor();
    await expect(page.locator('.ant-modal input').nth(0)).toHaveValue('Voucher E2E Test');
    await page.locator('.ant-modal input').nth(0).fill('Voucher E2E Edit');
    await clickText(page, '.ant-modal button', 'Lưu thay đổi');
    await expect
      .poll(() => page.locator('.ant-table-row').filter({ hasText: 'Voucher E2E Edit' }).count())
      .toBeGreaterThan(0);

    expect(await deleteRowWithText(page, 'Voucher E2E Edit', before)).toBe(true);
  });

  test('article: tạo bài viết', async ({ page }) => {
    await login(page);
    await page.goto('/admin/article', { waitUntil: 'networkidle' });
    const before = await dataRows(page);

    await page.locator('button[nzType="primary"]').click();
    await page.locator('.ant-drawer input').first().waitFor();
    const inputs = page.locator('.ant-drawer input');
    await inputs.nth(0).fill('Bài viết E2E');
    await inputs.nth(1).fill('https://placehold.co/600x300');
    await page.locator('.ant-drawer textarea').fill('Tóm tắt bài E2E');
    await page.locator('.ant-drawer .ql-editor').click();
    await page.keyboard.type('Nội dung bài viết E2E');
    const selects = page.locator('.ant-drawer .ant-select');
    await selects.nth(0).click();
    await pickOption(page);
    await selects.nth(1).click();
    await pickOption(page);

    expect(await clickText(page, '.ant-drawer button', 'Đăng bài')).toBe(true);
    await expect
      .poll(() => page.isVisible('.ant-drawer-open .ant-drawer-content-wrapper').catch(() => false))
      .toBe(false);
    await expect.poll(() => dataRows(page)).toBe(before + 1);
  });

  test('user: toggle trạng thái', async ({ page }) => {
    await login(page);
    await page.goto('/admin/user', { waitUntil: 'networkidle' });
    const row = page.locator('.ant-table-row').nth(2);
    await expect(row.locator('.ant-tag')).toHaveText(['Active']);
    await row.locator('button').click();
    await expect(row.locator('.ant-tag')).toHaveText(['Inactive']);
    await row.locator('button').click();
    await expect(row.locator('.ant-tag')).toHaveText(['Active']);
  });

  test('reservation: mở modal xem chi tiết', async ({ page }) => {
    await login(page);
    await page.goto('/admin/reservation', { waitUntil: 'networkidle' });
    await page.locator('.ant-table-row').nth(1).locator('button').nth(0).click();
    await expect(page.locator('.ant-modal-wrap')).toBeVisible();
  });

  test('checkin: tìm vé bằng mã', async ({ page }) => {
    await login(page);
    await page.goto('/admin/checkin', { waitUntil: 'networkidle' });
    await page.fill('input[placeholder="Nhập mã vé (VD: res-001)"]', 'res-001');
    await page.keyboard.press('Enter');
    const stub = page.locator('app-ticket-stub');
    await expect(stub).toBeVisible();
    await expect(stub).toContainText('res-001');
  });
});
