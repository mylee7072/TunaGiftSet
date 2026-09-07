import { lazy } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";
import { RootLayout } from "./components/layout/RootLayout";
import { AdminLayout } from "./components/admin/AdminLayout";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { AdminRoute } from "./routes/AdminRoute";

// Route-level code splitting: every page below is its own chunk, only fetched
// when its route is actually visited. Layouts/guards/providers above stay in
// the main bundle since they're needed on every route regardless.
const HomePage = lazy(() => import("./pages/HomePage").then((m) => ({ default: m.HomePage })));
const ProductListPage = lazy(() => import("./pages/ProductListPage").then((m) => ({ default: m.ProductListPage })));
const ProductDetailPage = lazy(() => import("./pages/ProductDetailPage").then((m) => ({ default: m.ProductDetailPage })));
const LoginPage = lazy(() => import("./pages/LoginPage").then((m) => ({ default: m.LoginPage })));
const SignupPage = lazy(() => import("./pages/SignupPage").then((m) => ({ default: m.SignupPage })));
const OAuthCallbackPage = lazy(() => import("./pages/OAuthCallbackPage").then((m) => ({ default: m.OAuthCallbackPage })));
const CartPage = lazy(() => import("./pages/CartPage").then((m) => ({ default: m.CartPage })));
const OrderFormPage = lazy(() => import("./pages/OrderFormPage").then((m) => ({ default: m.OrderFormPage })));
const OrderCompletePage = lazy(() => import("./pages/OrderCompletePage").then((m) => ({ default: m.OrderCompletePage })));
const PaymentSuccessPage = lazy(() => import("./pages/PaymentSuccessPage").then((m) => ({ default: m.PaymentSuccessPage })));
const PaymentFailPage = lazy(() => import("./pages/PaymentFailPage").then((m) => ({ default: m.PaymentFailPage })));
const MyPage = lazy(() => import("./pages/MyPage").then((m) => ({ default: m.MyPage })));
const MyOrdersPage = lazy(() => import("./pages/MyOrdersPage").then((m) => ({ default: m.MyOrdersPage })));
const MyOrderDetailPage = lazy(() => import("./pages/MyOrderDetailPage").then((m) => ({ default: m.MyOrderDetailPage })));
const MyCouponsPage = lazy(() => import("./pages/MyCouponsPage").then((m) => ({ default: m.MyCouponsPage })));
const MyAddressesPage = lazy(() => import("./pages/MyAddressesPage").then((m) => ({ default: m.MyAddressesPage })));
const MyWishlistPage = lazy(() => import("./pages/MyWishlistPage").then((m) => ({ default: m.MyWishlistPage })));
const CompanyInfoPage = lazy(() => import("./pages/CompanyInfoPage").then((m) => ({ default: m.CompanyInfoPage })));
const NotFoundPage = lazy(() => import("./pages/NotFoundPage").then((m) => ({ default: m.NotFoundPage })));

const AdminDashboardPage = lazy(() => import("./pages/admin/AdminDashboardPage").then((m) => ({ default: m.AdminDashboardPage })));
const AdminProductListPage = lazy(() => import("./pages/admin/AdminProductListPage").then((m) => ({ default: m.AdminProductListPage })));
const AdminProductFormPage = lazy(() => import("./pages/admin/AdminProductFormPage").then((m) => ({ default: m.AdminProductFormPage })));
const AdminCategoriesPage = lazy(() => import("./pages/admin/AdminCategoriesPage").then((m) => ({ default: m.AdminCategoriesPage })));
const AdminBrandsPage = lazy(() => import("./pages/admin/AdminBrandsPage").then((m) => ({ default: m.AdminBrandsPage })));
const AdminOrderListPage = lazy(() => import("./pages/admin/AdminOrderListPage").then((m) => ({ default: m.AdminOrderListPage })));
const AdminOrderDetailPage = lazy(() => import("./pages/admin/AdminOrderDetailPage").then((m) => ({ default: m.AdminOrderDetailPage })));
const AdminCouponsPage = lazy(() => import("./pages/admin/AdminCouponsPage").then((m) => ({ default: m.AdminCouponsPage })));

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="admin" element={<AdminRoute />}>
              <Route element={<AdminLayout />}>
                <Route index element={<AdminDashboardPage />} />
                <Route path="products" element={<AdminProductListPage />} />
                <Route path="products/new" element={<AdminProductFormPage />} />
                <Route path="products/:productId/edit" element={<AdminProductFormPage />} />
                <Route path="categories" element={<AdminCategoriesPage />} />
                <Route path="brands" element={<AdminBrandsPage />} />
                <Route path="orders" element={<AdminOrderListPage />} />
                <Route path="orders/:orderNumber" element={<AdminOrderDetailPage />} />
                <Route path="coupons" element={<AdminCouponsPage />} />
              </Route>
            </Route>

            <Route element={<RootLayout />}>
              <Route index element={<HomePage />} />
              <Route path="products" element={<ProductListPage />} />
              <Route path="products/:productId" element={<ProductDetailPage />} />
              <Route path="login" element={<LoginPage />} />
              <Route path="signup" element={<SignupPage />} />
              <Route path="auth/:provider/callback" element={<OAuthCallbackPage />} />
              <Route path="payment/success" element={<PaymentSuccessPage />} />
              <Route path="payment/fail" element={<PaymentFailPage />} />
              <Route path="company" element={<CompanyInfoPage />} />

              <Route element={<ProtectedRoute />}>
                <Route path="cart" element={<CartPage />} />
                <Route path="order" element={<OrderFormPage />} />
                <Route path="order/complete/:orderNumber" element={<OrderCompletePage />} />
                <Route path="mypage" element={<MyPage />} />
                <Route path="mypage/addresses" element={<MyAddressesPage />} />
                <Route path="mypage/wishlist" element={<MyWishlistPage />} />
                <Route path="mypage/orders" element={<MyOrdersPage />} />
                <Route path="mypage/orders/:orderNumber" element={<MyOrderDetailPage />} />
                <Route path="mypage/coupons" element={<MyCouponsPage />} />
              </Route>

              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}
