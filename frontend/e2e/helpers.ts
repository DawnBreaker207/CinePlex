import { expect, Page } from '@playwright/test';

export async function login(page: Page): Promise<void> {
  await page.goto('/login', { waitUntil: 'networkidle' });
  const inputs = page.locator('input');
  await inputs.nth(0).fill('admin');
  await inputs.nth(1).fill('admin');
  await page.getByRole('button', { name: /Đăng nhập/ }).click();
  await page.waitForTimeout(2000);
}

export async function clickText(
  page: Page,
  selector: string,
  text: string,
): Promise<boolean> {
  const els = page.locator(selector);
  const count = await els.count();
  for (let i = 0; i < count; i++) {
    if (((await els.nth(i).textContent()) ?? '').includes(text)) {
      await els.nth(i).click();
      return true;
    }
  }
  return false;
}

export async function pickOption(page: Page, index = 0): Promise<void> {
  await page
    .locator('.ant-select-dropdown:not(.ant-select-dropdown-hidden) .ant-select-item-option')
    .nth(index)
    .click();
}

export async function fillByPlaceholder(
  page: Page,
  partial: string,
  value: string,
  nth = 0,
  enter = false,
): Promise<boolean> {
  const inputs = page.locator('.ant-modal input, .ant-drawer input');
  const count = await inputs.count();
  let found = 0;
  for (let i = 0; i < count; i++) {
    const ph = (await inputs.nth(i).getAttribute('placeholder')) ?? '';
    if (ph.toLowerCase().includes(partial.toLowerCase())) {
      if (found === nth) {
        await inputs.nth(i).fill(value);
        if (enter) await inputs.nth(i).press('Enter');
        return true;
      }
      found++;
    }
  }
  return false;
}

export async function expectPlaceholderValue(
  page: Page,
  partial: string,
  value: string,
  nth = 0,
): Promise<void> {
  const inputs = page.locator('.ant-modal input, .ant-drawer input');
  const count = await inputs.count();
  let found = 0;
  for (let i = 0; i < count; i++) {
    const ph = (await inputs.nth(i).getAttribute('placeholder')) ?? '';
    if (ph.toLowerCase().includes(partial.toLowerCase())) {
      if (found === nth) {
        await expect(inputs.nth(i)).toHaveValue(value);
        return;
      }
      found++;
    }
  }
}

export async function dataRows(page: Page): Promise<number> {
  return (await page.locator('.ant-table-row').count()) - 1;
}

export async function modalVisible(page: Page): Promise<boolean> {
  return page.isVisible('.ant-modal-wrap').catch(() => false);
}

export async function deleteRowWithText(
  page: Page,
  rowText: string,
  expectedBefore: number,
): Promise<boolean> {
  await page
    .locator('.ant-table-row')
    .filter({ hasText: rowText })
    .first()
    .getByRole('button', { name: 'Xóa' })
    .click();
  await page.locator('.ant-modal-confirm-btns').waitFor();
  await clickText(page, '.ant-modal-confirm-btns button', 'Xóa');
  await page.waitForTimeout(1500);
  return (await dataRows(page)) === expectedBefore;
}
