import { render, screen } from "@testing-library/react";

import { StoreProvider } from "@/store/StoreProvider";

import { LocaleDocumentLanguage } from "./LocaleDocumentLanguage";

describe("LocaleDocumentLanguage", () => {
  it("updates the document language and renders its children", () => {
    document.documentElement.lang = "en";

    render(
      <StoreProvider>
        <LocaleDocumentLanguage lang="tr">
          <span>Content</span>
        </LocaleDocumentLanguage>
      </StoreProvider>,
    );

    expect(screen.getByText("Content")).toBeVisible();
    expect(document.documentElement.lang).toBe("tr");
  });

  it("updates the language when the prop changes", () => {
    const { rerender } = render(
      <StoreProvider>
        <LocaleDocumentLanguage lang="en">
          <span>Content</span>
        </LocaleDocumentLanguage>
      </StoreProvider>,
    );

    rerender(
      <StoreProvider>
        <LocaleDocumentLanguage lang="tr">
          <span>Content</span>
        </LocaleDocumentLanguage>
      </StoreProvider>,
    );

    expect(document.documentElement.lang).toBe("tr");
  });
});
