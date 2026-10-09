import { test, expect, type Page } from "@playwright/test";
async function lobby(page: Page, name: string) {
  await page.goto("/");
  await page
    .getByLabel("Display name")
    .fill(name + "-" + crypto.randomUUID().slice(0, 5));
  await page.getByRole("button", { name: "Enter the lobby" }).click();
  await expect(
    page.getByRole("button", { name: "Create a private room" }),
  ).toBeVisible();
}
async function roomCode(page: Page) {
  return (await page.locator(".room-code strong").innerText()).trim();
}
async function ready(page: Page) {
  await page
    .getByRole("button", { name: "Ready to play", exact: true })
    .click();
}

test("private match plays, rejects invalid squares, survives reload and resigns", async ({
  browser,
}) => {
  const host = await browser.newPage(),
    guest = await browser.newPage();
  try {
    await lobby(host, "Island host");
    await lobby(guest, "Island guest");
    await host.getByLabel("Game rules").selectOption("CLASSIC");
    await host.getByRole("button", { name: "Create a private room" }).click();
    const code = await roomCode(host);
    await guest.getByLabel("Have a room code?").fill("WRONG123");
    await guest.getByRole("button", { name: "Join", exact: true }).click();
    await expect(guest.getByRole("alert")).toContainText("No room found");
    await guest.getByLabel("Have a room code?").fill(code);
    await guest.getByRole("button", { name: "Join", exact: true }).click();
    await expect(guest.locator(".room-code strong")).toHaveText(code);
    await ready(host);
    await expect(guest.locator(".player-card").first()).toContainText("ready");
    await ready(guest);
    await expect(host.locator(".instruction")).toContainText(
      "Place your two workers",
    );
    await expect(guest.getByTestId("cell-0-0")).toBeDisabled();
    await host.getByTestId("cell-0-0").click();
    await host.getByTestId("cell-0-1").click();
    await guest.getByTestId("cell-4-4").click();
    await guest.getByTestId("cell-4-3").click();
    await host.getByTestId("cell-0-0").click();
    await expect(host.getByTestId("cell-4-4")).toBeDisabled();
    await host.getByTestId("cell-1-0").click();
    await host.getByTestId("cell-0-0").click();
    await expect(guest.getByTestId("cell-0-0")).toHaveAttribute(
      "aria-label",
      /level 1/,
    );
    await host.reload();
    await expect(host.locator(".room-code strong")).toHaveText(code);
    await expect(guest.getByTestId("cell-4-4")).toBeEnabled();
    await guest.getByTestId("cell-4-4").click();
    await guest.getByTestId("cell-3-4").click();
    await guest.getByTestId("cell-4-4").click();
    guest.once("dialog", (dialog) => dialog.accept());
    await guest.getByRole("button", { name: "Concede this match" }).click();
    await expect(host.locator(".instruction")).toContainText(
      /Island host.*wins/,
    );
    await host.getByRole("button", { name: "Back to lobby" }).click();
    await expect(
      host.getByRole("button", { name: "Find an opponent" }),
    ).toBeVisible();
  } finally {
    await host.close();
    await guest.close();
  }
});

test("public queue ignores private rooms and supports cancelling a search", async ({
  browser,
}) => {
  const privateHost = await browser.newPage(),
    first = await browser.newPage(),
    second = await browser.newPage();
  try {
    await lobby(privateHost, "Private host");
    await lobby(first, "Public first");
    await lobby(second, "Public second");
    await privateHost
      .getByRole("button", { name: "Create a private room" })
      .click();
    const privateCode = await roomCode(privateHost);
    await first.getByRole("button", { name: "Find an opponent" }).click();
    await expect(first.locator(".instruction")).toContainText(
      "Waiting for a second player",
    );
    expect(await roomCode(first)).not.toBe(privateCode);
    await first.getByRole("button", { name: "Leave room" }).click();
    await first.getByRole("button", { name: "Find an opponent" }).click();
    await second.getByRole("button", { name: "Find an opponent" }).click();
    await expect(first.locator(".player-card")).toHaveCount(2);
    expect(await roomCode(second)).toBe(await roomCode(first));
    await first.getByLabel("Your god power").selectOption("Triton");
    await expect(second.locator(".player-card").first()).toContainText(
      "Triton",
    );
  } finally {
    await privateHost.close();
    await first.close();
    await second.close();
  }
});

test("mobile lobby and board fit the viewport", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await lobby(page, "Mobile player");
  await page.getByRole("button", { name: "Create a private room" }).click();
  await expect(page.locator(".isometric-board")).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
});

test("accounts reserve their names and can log back in after logout", async ({
  browser,
}) => {
  const accountName = "Account-" + crypto.randomUUID().slice(0, 8);
  const owner = await browser.newPage(),
    guest = await browser.newPage();
  try {
    await owner.goto("/");
    await owner
      .getByRole("button", { name: "Create account", exact: true })
      .click();
    await owner.getByLabel("Account name").fill(accountName);
    await owner
      .getByLabel("Password", { exact: true })
      .fill("a rather sturdy password");
    await owner
      .getByRole("button", { name: "Create account and take a seat" })
      .click();
    await expect(owner.getByRole("status")).toContainText(
      `${accountName} · account`,
    );
    await guest.goto("/");
    await guest.getByLabel("Display name").fill(accountName.toLowerCase());
    await guest.getByRole("button", { name: "Enter the lobby" }).click();
    await expect(guest.getByRole("alert")).toContainText("already belongs");
    await owner.getByRole("button", { name: "Log out" }).click();
    await owner.getByRole("button", { name: "Log in", exact: true }).click();
    await owner.getByLabel("Account name").fill(accountName);
    await owner
      .getByLabel("Password", { exact: true })
      .fill("a rather sturdy password");
    await owner.getByRole("button", { name: "Log in and take a seat" }).click();
    await expect(owner.getByRole("status")).toContainText(
      `${accountName} · account`,
    );
  } finally {
    await owner.close();
    await guest.close();
  }
});
