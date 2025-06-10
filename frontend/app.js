// React frontend with router
const { HashRouter, Switch, Route, Link, Redirect } = ReactRouterDOM;

// globally store auth token so helper fetch works across components
let authToken = localStorage.getItem('accessToken') || '';

function setAuthToken(token) {
  authToken = token;
  if (token) {
    localStorage.setItem('accessToken', token);
  } else {
    localStorage.removeItem('accessToken');
  }
}

// helper fetch that automatically applies Authorization header when token exists
function authFetch(url, options = {}) {
  const headers = Object.assign({}, options.headers);
  if (authToken) {
    headers['Authorization'] = `Bearer ${authToken}`;
  }
  return fetch(url, { ...options, headers });
}

function Login({ onLogin }) {
  const [email, setEmail] = React.useState('');
  const [password, setPassword] = React.useState('');
  const [error, setError] = React.useState('');

  const login = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const res = await fetch('/users/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      if (!res.ok) throw new Error('Login failed');
      const data = await res.json();
      const accessToken = data.data.accessToken;
      onLogin(accessToken);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <form onSubmit={login} className="login-form">
      <input value={email} onChange={e => setEmail(e.target.value)} placeholder="Email" />
      <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Password" />
      <button type="submit">Login</button>
      {error && <p className="error">{error}</p>}
    </form>
  );
}

function Home() {
  return <p>Select a menu option.</p>;
}

function Product() {
  const [productId, setProductId] = React.useState('');
  const [product, setProduct] = React.useState(null);
  const [error, setError] = React.useState('');

  const fetchProduct = async () => {
    if (!productId) return;
    setError('');
    try {
      const res = await authFetch(`/products/${productId}`);
      if (!res.ok) throw new Error('Failed to fetch product');
      const data = await res.json();
      setProduct(data.data);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div>
      <input value={productId} onChange={e => setProductId(e.target.value)} placeholder="Product ID" />
      <button onClick={fetchProduct}>Get Product</button>
      {product && <pre>{JSON.stringify(product, null, 2)}</pre>}
      {error && <p className="error">{error}</p>}
    </div>
  );
}

function CategoryList() {
  const [categories, setCategories] = React.useState([]);
  const [error, setError] = React.useState('');
  const load = async () => {
    setError('');
    try {
      const res = await authFetch('/categories?page=0&size=20');
      if (!res.ok) throw new Error('Failed to fetch categories');
      const data = await res.json();
      setCategories(data.data.content || []);
    } catch (err) {
      setError(err.message);
    }
  };
  return (
    <div>
      <button onClick={load}>Load Categories</button>
      <pre>{JSON.stringify(categories, null, 2)}</pre>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

function BrandList() {
  const [brands, setBrands] = React.useState([]);
  const [error, setError] = React.useState('');
  const load = async () => {
    setError('');
    try {
      const res = await authFetch('/brands?page=0&size=20');
      if (!res.ok) throw new Error('Failed to fetch brands');
      const data = await res.json();
      setBrands(data.data.content || []);
    } catch (err) {
      setError(err.message);
    }
  };
  return (
    <div>
      <button onClick={load}>Load Brands</button>
      <pre>{JSON.stringify(brands, null, 2)}</pre>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

function Cart() {
  const [items, setItems] = React.useState([]);
  const [error, setError] = React.useState('');
  const load = async () => {
    setError('');
    try {
      const res = await authFetch('/carts/users?page=0&size=20');
      if (!res.ok) throw new Error('Failed to fetch cart');
      const data = await res.json();
      setItems(data.data.content || []);
    } catch (err) {
      setError(err.message);
    }
  };
  return (
    <div>
      <button onClick={load}>Load Cart</button>
      <pre>{JSON.stringify(items, null, 2)}</pre>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

function Orders() {
  const [orders, setOrders] = React.useState([]);
  const [error, setError] = React.useState('');
  const load = async () => {
    setError('');
    try {
      const res = await authFetch('/orders/users?page=0&size=20');
      if (!res.ok) throw new Error('Failed to fetch orders');
      const data = await res.json();
      setOrders(data.data.content || []);
    } catch (err) {
      setError(err.message);
    }
  };
  return (
    <div>
      <button onClick={load}>Load Orders</button>
      <pre>{JSON.stringify(orders, null, 2)}</pre>
      {error && <p className="error">{error}</p>}
    </div>
  );
}

function App() {
  const [token, setToken] = React.useState(authToken);

  const onLogin = (newToken) => {
    setToken(newToken);
    setAuthToken(newToken);
  };

  const logout = () => {
    setToken('');
    setAuthToken('');
  };

  return (
    <HashRouter>
      <div className="app">
        <h1>Soo Shinsa Frontend</h1>
        {token ? (
          <>
            <nav className="nav">
              <Link to="/">Home</Link>
              <Link to="/product">Product</Link>
              <Link to="/categories">Categories</Link>
              <Link to="/brands">Brands</Link>
              <Link to="/cart">Cart</Link>
              <Link to="/orders">Orders</Link>
              <button onClick={logout}>Logout</button>
            </nav>
            <Switch>
              <Route exact path="/" component={Home} />
              <Route path="/product" component={Product} />
              <Route path="/categories" component={CategoryList} />
              <Route path="/brands" component={BrandList} />
              <Route path="/cart" component={Cart} />
              <Route path="/orders" component={Orders} />
              <Redirect to="/" />
            </Switch>
          </>
        ) : (
          <Login onLogin={onLogin} />
        )}
      </div>
    </HashRouter>
  );
}

ReactDOM.render(<App />, document.getElementById('root'));
