// src/routes.js
import React from 'react';
import HomePage from './pages/Home';
import TrainsPage from './pages/Trains';
import OrdersPage from './pages/Orders';
import MyTicketsPage from './pages/MyTickets';
import ProfilePage from './pages/Profile';
import LoginPage from './pages/Login';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import SubmitOrderPage from './pages/SubmitOrder';
import ChangeTicketPage from './pages/ChangeTicket';
import AddPassengerPage from './pages/AddPassenger';
import ReturnTicketPage from './pages/ReturnTicket';
import OrderDetailPage from './pages/OrderDetail';
import TicketDetailPage from './pages/TicketDetail';
import PaymentPage from './pages/Payment';

// 更新 PrivateRoute 组件，使用 Redux 状态
const PrivateRoute = ({ element, redirectPath = '/login' }) => {
  const { isAuthenticated } = useAuth(); // 现在 useAuth 从 Redux 获取状态

  if (!isAuthenticated) {
    return <Navigate to={redirectPath} state={{ from: window.location.pathname }} replace />;
  }

  return element;
};

// 更新 LoginRedirectHandler
const LoginRedirectHandler = () => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  React.useEffect(() => {
    if (isAuthenticated) {
      const from = location.state?.from || '/';
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, location, navigate]);

  return <LoginPage />;
};
/*
// 注册后重定向逻辑
const RegisterRedirectHandler = () => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  React.useEffect(() => {
    if (isAuthenticated) {
      const from = location.state?.from || '/';
      const referrer = document.referrer;
      const isFromLoginPage = referrer && referrer.includes('/login');
      
      if (isFromLoginPage && location.state?.from) {
        navigate(location.state.from, { replace: true });
      } else {
        navigate(from, { replace: true });
      }
    }
  }, [isAuthenticated, location, navigate]);

  return <RegisterPage />;
};
*/


// 封装所有需要认证的路由
const authRoutes = [
  {
    path: '/orders',
    element: <OrdersPage />,
    name: '我的订单',
  },
  {
    path: '/my-tickets',
    element: <MyTicketsPage />,
    name: '我的车票',
  },
  {
    path: '/profile',
    element: <ProfilePage />,
    name: '个人中心',
  },
];

export const routes = [
  {
    path: '/',
    element: <HomePage />,
    name: '首页'
  },
  {
    path: '/trains',
    element: <TrainsPage />,
    name: '车票预订'
  },
  {
    path: '/login',
    element: <LoginRedirectHandler />,
    name: '登录'
  },
    /*
  {
    path: '/register',
    element: <RegisterRedirectHandler />,
    name: '注册'
  },
  */
  {
    path: '/add-passenger',
    element: <AddPassengerPage />,
    name: '添加乘车人',
  },
  {
    path: '/submit-order',
    element: <SubmitOrderPage />,
    name: '提交订单',
  },
  {
    path: '/change-ticket',
    element: <ChangeTicketPage />,
    name: '改签',
  },
  {
    path: '/return-ticket',
    element: <ReturnTicketPage />,
    name: '退票',
  },
  {
    path: '/order-detail',
    element: <OrderDetailPage />,
    name: '订单详情',
  },
  {
    path: '/ticket-detail',
    element: <TicketDetailPage />,
    name: '车票详情'
  },

  {
    path: '/payment',
    element: <PaymentPage />,
    name: '订单支付'
  },


  ...authRoutes.map(route => ({
    ...route,
    element: <PrivateRoute element={route.element} />,
    requiresAuth: true
  }))
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

