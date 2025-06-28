// src/routes.js
import React from 'react';
import HomePage from './pages/Home';
import TicketsPage from './pages/Tickets';
import OrdersPage from './pages/Orders';
import MyTicketsPage from './pages/MyTickets';
import ProfilePage from './pages/Profile';
import LoginPage from './pages/Login';
import RegisterPage from './pages/Register';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from './hooks/useAuth';
import SubmitOrderPage from './pages/SubmitOrder';
import ChangeTicketPage from './pages/ChangeTicket';
import AddPassengerPage from './pages/AddPassenger';

// 封装需要登录的组件
const PrivateRoute = ({ element, redirectPath = '/login' }) => {
  const checkAuthenticated = () => {
    try {
      const userData = localStorage.getItem('mini12306_user');
      const loginTimestamp = localStorage.getItem('mini12306_login_time');
      return !!(userData && loginTimestamp);
    } catch (e) {
      console.error("Error checking auth state", e);
      return false;
    }
  };

  const isAuth = checkAuthenticated();
  console.log('PrivateRoute checking auth (direct):', isAuth);

  if (!isAuth) {
    console.log('Not authenticated, redirecting to login with state:', { from: window.location.pathname });
    return <Navigate to={redirectPath} state={{ from: window.location.pathname }} replace />;
  }
  
  return element;
};

// 登录后重定向逻辑
const LoginRedirectHandler = () => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  React.useEffect(() => {
    console.log('LoginRedirectHandler auth state:', isAuthenticated);
    const loginTimestamp = localStorage.getItem('mini12306_login_time');
    
    if (isAuthenticated && loginTimestamp) {
      const from = location.state?.from || '/';
      console.log('Already authenticated, redirecting to:', from);
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, location, navigate]);

  return <LoginPage />;
};

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
    path: '/tickets',
    element: <TicketsPage />,
    name: '车票预订'
  },
  {
    path: '/login',
    element: <LoginRedirectHandler />,
    name: '登录'
  },
  {
    path: '/register',
    element: <RegisterRedirectHandler />,
    name: '注册'
  },
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

