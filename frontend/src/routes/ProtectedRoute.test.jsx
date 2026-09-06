import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { AuthContext } from "../context/authContextValue";
import { ProtectedRoute } from "./ProtectedRoute";

function renderProtected(authValue) {
  render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter initialEntries={["/cart"]}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/cart" element={<div>장바구니 본문</div>} />
          </Route>
          <Route path="/login" element={<div>로그인 페이지</div>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

describe("ProtectedRoute", () => {
  it("renders protected content when authenticated", () => {
    renderProtected({ isAuthenticated: true, isLoading: false });
    expect(screen.getByText("장바구니 본문")).toBeInTheDocument();
  });

  it("redirects unauthenticated users to login", () => {
    renderProtected({ isAuthenticated: false, isLoading: false });
    expect(screen.getByText("로그인 페이지")).toBeInTheDocument();
  });

  it("shows loading while auth state is being restored", () => {
    renderProtected({ isAuthenticated: false, isLoading: true });
    expect(screen.getByText("로그인 상태를 확인하고 있습니다...")).toBeInTheDocument();
  });
});
