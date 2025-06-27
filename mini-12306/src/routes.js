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
// import SubmitOrderPage from './pages/SubmitOrder';
// import PaymentPage from './pages/Payment';
// import OrderDetailPage from './pages/OrderDetail';
// import TicketDetailPage from './pages/TicketDetail';
// import ChangeTicketPage from './pages/ChangeTicket';
// import RefundPage from './pages/Refund';

// 封装需要登录的组件
const PrivateRoute = ({ element, redirectPath = '/login' }) => {
  // 不使用useAuth的方式，直接从localStorage读取
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
    // 未登录时，重定向到登录页面，并传递当前路径
    console.log('Not authenticated, redirecting to login with state:', { from: window.location.pathname });
    return <Navigate to={redirectPath} state={{ from: window.location.pathname }} replace />;
  }
  
  return element;
};

//登录后重定向逻辑
const LoginRedirectHandler = () => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  React.useEffect(() => {
    console.log('LoginRedirectHandler auth state:', isAuthenticated);
    // 检查本地存储的登录时间戳，确保我们真正处于登录状态
    const loginTimestamp = localStorage.getItem('mini12306_login_time');
    
    if (isAuthenticated && loginTimestamp) {
      const from = location.state?.from || '/';
      console.log('Already authenticated, redirecting to:', from);
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, location, navigate]);

  return <LoginPage />;
};

//注册后重定向逻辑
const RegisterRedirectHandler = () => {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  React.useEffect(() => {
    if (isAuthenticated) {
      // 如果已登录，从登录页来的应该回到登录页指定的from
      // 否则回到首页
      const from = location.state?.from || '/';
      
      // 检查referrer是否是登录页
      const referrer = document.referrer;
      const isFromLoginPage = referrer && referrer.includes('/login');
      
      if (isFromLoginPage && location.state?.from) {
        // 如果从登录页来且有指定跳转页，就跳到那个页
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
  // 其他需要认证的路由...
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
  // 使用map将所有需要认证的路由包装在PrivateRoute中
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