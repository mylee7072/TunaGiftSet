import { BrowserRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ToastProvider } from "./context/ToastContext";
import { RootLayout } from "./components/layout/RootLayout";
import { AdminLayout } from "./components/admin/AdminLayout";
import { ProtectedRoute } from "./routes/ProtectedRoute";
import { AdminRoute } from "./routes/AdminRoute";
import { HomePage } from "./pages/HomePage";
import { ProductListPage } from "./pages/ProductListPage";
import { ProductDetailPage } from "./pages/ProductDetailPage";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { CartPage } from "./pages/CartPage";
import { OrderFormPage } from "./pages/OrderFormPage";
import { OrderCompletePage } from "./pages/OrderCompletePage";
import { PaymentSuccessPage } from "./pages/PaymentSuccessPage";
import { PaymentFailPage } from "./pages/PaymentFailPage";
import { MyPage } from "./pages/MyPage";
import { MyOrdersPage } from "./pages/MyOrdersPage";
import { MyOrderDetailPage } from "./pages/MyOrderDetailPage";
import { MyCouponsPage } from "./pages/MyCouponsPage";
import { MyAddressesPage } from "./pages/MyAddressesPage";
import { MyWishlistPage } from "./pages/MyWishlistPage";
import { CompanyInfoPage } from "./pages/CompanyInfoPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { AdminDashboardPage } from "./pages/admin/AdminDashboardPage";
import { AdminProductListPage } from "./pages/admin/AdminProductListPage";
import { AdminProductFormPage } from "./pages/admin/AdminProductFormPage";
import { AdminCategoriesPage } from "./pages/admin/AdminCategoriesPage";
import { AdminBrandsPage } from "./pages/admin/AdminBrandsPage";
import { AdminOrderListPage } from "./pages/admin/AdminOrderListPage";
import { AdminOrderDetailPage } from "./pages/admin/AdminOrderDetailPage";
import { AdminCouponsPage } from "./pages/admin/AdminCouponsPage";

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
