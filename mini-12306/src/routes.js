// src/routes.js
import React from 'react';
import HomePage from './pages/Home';
import TicketsPage from './pages/Tickets';
import OrdersPage from './pages/Orders';
import MyTicketsPage from './pages/MyTickets';
import ProfilePage from './pages/Profile';
// import LoginPage from './pages/Login';
// import RegisterPage from './pages/Register';
// import SubmitOrderPage from './pages/SubmitOrder';
// import PaymentPage from './pages/Payment';
// import OrderDetailPage from './pages/OrderDetail';
// import TicketDetailPage from './pages/TicketDetail';
// import ChangeTicketPage from './pages/ChangeTicket';
// import RefundPage from './pages/Refund';
// import { Navigate } from 'react-router-dom';
// import { useAuth } from './hooks/useAuth';

/*
// 封装需要登录的组件
const PrivateRoute = ({ element, redirectPath = '/login' }) => {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? element : <Navigate to={redirectPath} state={{ from: window.location.pathname }} replace />;
};

// 封装登录后重定向逻辑
const LoginRedirectHandler = () => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  if (isAuthenticated) {
    const from = location.state?.from || '/';
    navigate(from, { replace: true });
  }

  return <LoginPage />;
};
*/

export const routes = [
    {
        path: '/',
        element: <HomePage />,
        name: '首页'
    },
    {
        path: '/tickets',
        element: <TicketsPage />,
        // element: (
        //   <SearchRedirect>
        //     <TicketsPage />
        //   </SearchRedirect>
        // ),
        name: '车票预订'
    },
    /*
    {
      path: '/login',
      element: <LoginRedirectHandler />,
      name: '登录'
    },
    {
      path: '/register',
      element: <RegisterPage />,
      name: '注册'
    },
    */
    {
        path: '/orders',
        element: <OrdersPage />,
        // element: <PrivateRoute element={<OrdersPage />} />,
        name: '我的订单',
        // requiresAuth: true
    },
    /*
    {
      path: '/orders/:id',
      element: <PrivateRoute element={<OrderDetailPage />} />,
      name: '订单详情',
      // requiresAuth: true
    },
    */
    {
        path: '/my-tickets',
        element: <MyTicketsPage />,
        // element: <PrivateRoute element={<MyTicketsPage />} />,
        name: '我的车票',
        // requiresAuth: true
    },
    /*
    {
      path: '/my-tickets/:id',
      element: <PrivateRoute element={<TicketDetailPage />} />,
      name: '车票详情',
      // requiresAuth: true
    },
    */
    {
        path: '/profile',
        element: <ProfilePage />,
        // element: <PrivateRoute element={<ProfilePage />} />,
        name: '个人中心',
        // requiresAuth: true
    },
    /*
    {
      path: '/submit-order',
      element: <PrivateRoute element={<SubmitOrderPage />} />,
      name: '提交订单',
      // requiresAuth: true
    },
    {
      path: '/payment',
      element: <PrivateRoute element={<PaymentPage />} />,
      name: '支付',
      // requiresAuth: true
    },
    {
      path: '/change-ticket',
      element: <PrivateRoute element={<ChangeTicketPage />} />,
      name: '改签',
      // requiresAuth: true
    },
    {
      path: '/refund',
      element: <PrivateRoute element={<RefundPage />} />,
      name: '退票',
      // requiresAuth: true
    }
    */
];

/*
// 搜索重定向组件（用于首页搜索跳转）
const SearchRedirect = ({ children }) => {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);

  // 如果没有搜索参数，重定向回首页
  if (!searchParams.has('from') || !searchParams.has('to') || !searchParams.has('date')) {
    return <Navigate to="/" replace />;
  }

  return children;
};
*/

/*
// 按钮跳转逻辑示例（实际应放在组件中）
const handleBuyTicket = (ticketId) => {
  // if (!isAuthenticated) {
  //   navigate('/login', { state: { from: `/submit-order?ticketId=${ticketId}` } });
  // } else {
  //   navigate(`/submit-order?ticketId=${ticketId}`);
  // }
};

const handlePayment = (orderId) => {
  // navigate(`/payment?orderId=${orderId}`);
};

const handleCancelOrder = (isDetailPage) => {
  // navigate(isDetailPage ? '/orders' : -1);
};
*/