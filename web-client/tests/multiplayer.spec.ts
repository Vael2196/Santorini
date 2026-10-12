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
    await host.getByRole("button", { name: "Settings", exact: true }).click();
    await host.getByLabel("Show tower levels").check();
    await host.getByRole("button", { name: "Settings", exact: true }).click();
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
    await expect(host.locator(".level-marker")).toHaveText("1");
    expect(
      await host
        .getByTestId("cell-0-0")
        .evaluate((tile) => getComputedStyle(tile).outlineStyle),
    ).toBe("none");
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
  await page.getByRole("button", { name: "Settings", exact: true }).click();
  await page.getByLabel("In-game cursor colour").selectOption("charcoal");
  await page.getByLabel("Show tower levels").check();
  await expect(page.locator(".board-panel .isometric-board")).toHaveAttribute(
    "data-cursor-colour",
    "charcoal",
  );
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
      .getByRole("button", { name: "Create account and play" })
      .click();
    await expect(owner.getByRole("status")).toContainText(
      `${accountName} · account`,
    );
    await owner.getByRole("button", { name: "Settings", exact: true }).click();
    await owner.getByLabel("In-game cursor colour").selectOption("teal");
    await owner.getByLabel("Show tower levels").check();
    await owner.getByRole("button", { name: "Sound volume" }).click();
    await owner.getByRole("slider").fill("35");
    await owner.getByRole("button", { name: "Increase volume" }).click();
    await owner.getByRole("slider").press("Escape");
    await owner.getByRole("button", { name: "Mute all sounds" }).click();
    await expect(
      owner.getByText("Saved to your account", { exact: true }),
    ).toBeVisible();
    await owner.evaluate(() => localStorage.clear());
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
    await owner.getByRole("button", { name: "Log in and play" }).click();
    await expect(owner.getByRole("status")).toContainText(
      `${accountName} · account`,
    );
    await expect(owner.getByLabel("In-game cursor colour")).toHaveValue("teal");
    await expect(owner.getByLabel("Show tower levels")).toBeChecked();
    await owner.getByRole("button", { name: "Sound volume" }).click();
    await expect(owner.getByRole("slider")).toHaveValue("40");
    await owner.getByRole("slider").press("Escape");
    await expect(
      owner.getByRole("button", { name: "Enable all sounds" }),
    ).toBeVisible();
  } finally {
    await owner.close();
    await guest.close();
  }
});

test("volume supports dragging and keyboard input, and handwriting and favicon load", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await lobby(page, "Volume");
  await page.getByRole("button", { name: "Sound volume" }).click();
  const slider = page.getByRole("slider");
  const box = await slider.boundingBox();
  expect(box).not.toBeNull();
  await page.mouse.move(box!.x + box!.width * 0.8, box!.y + box!.height / 2);
  await page.mouse.down();
  await page.mouse.move(box!.x + box!.width * 0.4, box!.y + box!.height / 2, {
    steps: 8,
  });
  await page.mouse.up();
  const changed = Number(await slider.inputValue());
  expect(changed).toBeGreaterThan(20);
  expect(changed).toBeLessThan(60);
  await page.getByRole("button", { name: "Increase volume" }).click();
  expect(Number(await slider.inputValue())).toBe(changed + 5);
  await slider.focus();
  await slider.press("ArrowLeft");
  expect(Number(await slider.inputValue())).toBe(changed + 4);
  await slider.press("Escape");
  await expect(
    page.getByRole("dialog", { name: "Sound volume" }),
  ).not.toBeVisible();
  await page.reload();
  await page.getByRole("button", { name: "Sound volume" }).click();
  await expect(slider).toHaveValue(String(changed + 4));
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  expect(
    await page.evaluate(async () => {
      await document.fonts.ready;
      return (
        document.fonts.check("16px Kalam") &&
        getComputedStyle(document.body).fontFamily.includes("Kalam")
      );
    }),
  ).toBe(true);
  const favicon = await page.request.get("/favicon.png");
  expect(favicon.ok()).toBe(true);
  expect(favicon.headers()["content-type"]).toContain("image/png");
});

test("solo practice advances the computer's placements and turn", async ({
  page,
}) => {
  await lobby(page, "Practice");
  await page.getByLabel("Game rules").selectOption("CLASSIC");
  await page.getByRole("button", { name: "Play against the computer" }).click();
  await expect(page.locator(".player-card")).toHaveCount(2);
  await ready(page);
  await page.getByTestId("cell-0-0").click();
  await page.getByTestId("cell-0-1").click();
  await expect(page.locator(".instruction")).toContainText(
    "Choose a highlighted worker",
    { timeout: 5000 },
  );
  await expect(page.locator(".piece-1")).toHaveCount(2);
  await page.getByTestId("cell-0-0").click();
  await page.getByTestId("cell-1-0").click();
  await page.getByTestId("cell-0-0").click();
  await expect(page.locator(".instruction")).toContainText(
    "Choose a highlighted worker",
    { timeout: 5000 },
  );
  await expect(
    page.getByRole("region", { name: "Room chat" }),
  ).not.toBeVisible();
});

for (const [mode, count, workers] of [
  ["THREE", 3, 2],
  ["FOUR_FFA", 4, 1],
  ["TEAMS", 4, 1],
] as const) {
  test(`${mode} plays with every seat and handles a forfeit`, async ({
    browser,
  }) => {
    const pages: Page[] = [];
    try {
      for (let i = 0; i < count; i++) {
        const p = await browser.newPage();
        pages.push(p);
        await lobby(p, "Mode player " + i);
      }
      const host = pages[0];
      await host.getByLabel("Players and teams").selectOption(mode);
      if (mode === "THREE")
        await host.getByLabel("Game rules").selectOption("CLASSIC");
      else await expect(host.getByLabel("Game rules")).toHaveValue("CLASSIC");
      await host.getByRole("button", { name: "Create a private room" }).click();
      const code = await roomCode(host);
      for (const p of pages.slice(1)) {
        await p.getByLabel("Have a room code?").fill(code);
        await p.getByRole("button", { name: "Join", exact: true }).click();
        await expect(p.locator(".room-code strong")).toHaveText(code);
      }
      await expect(host.locator(".player-card")).toHaveCount(count);
      for (const p of pages) await ready(p);
      for (let seat = 0; seat < count; seat++)
        for (let col = 0; col < workers; col++)
          await pages[seat].getByTestId(`cell-${seat}-${col}`).click();
      await expect(host.locator(".piece")).toHaveCount(count * workers);
      await expect(host.locator(".piece-2")).toHaveCount(workers);
      pages[1].once("dialog", (d) => d.accept());
      await pages[1]
        .getByRole("button", { name: "Concede this match" })
        .click();
      if (mode === "TEAMS")
        await expect(host.locator(".instruction")).toContainText(" and ");
      else {
        await expect(host.locator(".piece-1")).toHaveCount(0);
        await expect(
          host.getByRole("button", { name: "Concede this match" }),
        ).toBeEnabled();
      }
    } finally {
      for (const p of pages) await p.close();
    }
  });
}

test("ranked accounts settle ratings, exchange plain-text chat and appear on the leaderboard", async ({
  browser,
}) => {
  const a = await browser.newPage(),
    b = await browser.newPage();
  const names = [
    "Rated-A-" + crypto.randomUUID().slice(0, 6),
    "Rated-B-" + crypto.randomUUID().slice(0, 6),
  ];
  try {
    for (const [i, p] of [a, b].entries()) {
      await p.goto("/");
      await p
        .getByRole("button", { name: "Create account", exact: true })
        .click();
      await p.getByLabel("Account name").fill(names[i]);
      await p
        .getByLabel("Password", { exact: true })
        .fill("a rather sturdy password");
      await p.getByRole("button", { name: "Create account and play" }).click();
      await expect(p.locator(".rank-summary")).toContainText("1200");
      await p.getByLabel("Ranked match", { exact: true }).check();
      await p.getByRole("button", { name: "Find an opponent" }).click();
    }
    await expect(a.locator(".player-card")).toHaveCount(2);
    await ready(a);
    await ready(b);
    await a
      .getByLabel("Message", { exact: true })
      .fill("<img src=x> good game");
    await a.getByRole("button", { name: "Send message" }).click();
    await expect(b.getByRole("log")).toContainText("<img src=x> good game");
    await expect(b.getByRole("log").locator("img")).toHaveCount(0);
    b.once("dialog", (d) => d.accept());
    await b.getByRole("button", { name: "Concede this match" }).click();
    await expect(a.locator(".rank-result")).toContainText("1200 → 1216 (+16)");
    await expect(b.locator(".rank-result")).toContainText("1200 → 1184 (-16)");
    await a.getByRole("button", { name: "Leaderboard", exact: true }).click();
    await expect(
      a.getByRole("row").filter({ hasText: names[0] }),
    ).toContainText("1216");
    await a.reload();
    await expect(a.locator(".rank-result")).toContainText("1216");
  } finally {
    await a.close();
    await b.close();
  }
});
