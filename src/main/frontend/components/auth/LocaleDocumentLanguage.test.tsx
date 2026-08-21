import { render, screen } from "@testing-library/react";

import { LocaleDocumentLanguage } from "./LocaleDocumentLanguage";

describe("LocaleDocumentLanguage", () => {
  it("updates the document language and renders its children", () => {
    document.documentElement.lang = "en";

    render(
      <LocaleDocumentLanguage lang="tr">
        <span>Content</span>
      </LocaleDocumentLanguage>,
    );

    expect(screen.getByText("Content")).toBeVisible();
    expect(document.documentElement.lang).toBe("tr");
  });

  it("updates the language when the prop changes", () => {
    const { rerender } = render(
      <LocaleDocumentLanguage lang="en">
        <span>Content</span>
      </LocaleDocumentLanguage>,
    );

    rerender(
      <LocaleDocumentLanguage lang="tr">
        <span>Content</span>
      </LocaleDocumentLanguage>,
    );

    expect(document.documentElement.lang).toBe("tr");
  });
});
