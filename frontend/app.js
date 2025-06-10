function App() {
  const [token, setToken] = React.useState(localStorage.getItem('accessToken') || '');
  const [view, setView] = React.useState(token ? 'home' : 'login');

  const [email, setEmail] = React.useState('');
  const [password, setPassword] = React.useState('');

  const [productId, setProductId] = React.useState('');
  const [product, setProduct] = React.useState(null);
  const [categories, setCategories] = React.useState([]);
  const [brands, setBrands] = React.useState([]);
  const [cartItems, setCartItems] = React.useState([]);
  const [orders, setOrders] = React.useState([]);
  const [error, setError] = React.useState('');

  const authHeaders = token ? { 'Authorization': `Bearer ${token}` } : {};

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
      setToken(accessToken);
      localStorage.setItem('accessToken', accessToken);
      setView('home');
    } catch (err) {
      setError(err.message);
    }
  };

  const logout = () => {
    setToken('');
    localStorage.removeItem('accessToken');
    setView('login');
  };

  const fetchProduct = async () => {
    if (!productId) return;
    setError('');
    try {
      const res = await fetch(`/products/${productId}`, { headers: authHeaders });
      if (!res.ok) throw new Error('Failed to fetch product');
      const data = await res.json();
      setProduct(data.data);
    } catch (err) {
      setError(err.message);
    }
  };

  const fetchCategories = async () => {
    setError('');
    try {
      const res = await fetch('/categories?page=0&size=20');
      if (!res.ok) throw new Error('Failed to fetch categories');
      const data = await res.json();
      setCategories(data.data.content || []);
    } catch (err) {
      setError(err.message);
    }
  };

  const fetchBrands = async () => {
    setError('');
    try {
      const res = await fetch('/brands?page=0&size=20');
      if (!res.ok) throw new Error('Failed to fetch brands');
      const data = await res.json();
      setBrands(data.data.content || []);
    } catch (err) {
      setError(err.message);
    }
  };

  const fetchCart = async () => {
    setError('');
    try {
      const res = await fetch('/carts/users?page=0&size=20', { headers: { ...authHeaders, 'Content-Type': 'application/json' }, method: 'GET', body: JSON.stringify({}) });
      if (!res.ok) throw new Error('Failed to fetch cart');
      const data = await res.json();
      setCartItems(data.data.content || []);
    } catch (err) {
      setError(err.message);
    }
  };

  const fetchOrders = async () => {
    setError('');
    try {
      const res = await fetch('/orders/users?page=0&size=20', { headers: { ...authHeaders, 'Content-Type': 'application/json' }, method: 'GET', body: JSON.stringify({}) });
      if (!res.ok) throw new Error('Failed to fetch orders');
      const data = await res.json();
      setOrders(data.data.content || []);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="app">
      <h1>Soo Shinsa Frontend</h1>
      {token ? (
        <div>
          <nav className="nav">
            <button onClick={() => setView('home')}>Home</button>
            <button onClick={() => setView('product')}>Product</button>
            <button onClick={() => setView('categories')}>Categories</button>
            <button onClick={() => setView('brands')}>Brands</button>
            <button onClick={() => setView('cart')}>Cart</button>
            <button onClick={() => setView('orders')}>Orders</button>
            <button onClick={logout}>Logout</button>
          </nav>
          {view === 'home' && <p>Select a menu option.</p>}
          {view === 'product' && (
            <div>
              <input value={productId} onChange={e => setProductId(e.target.value)} placeholder="Product ID" />
              <button onClick={fetchProduct}>Get Product</button>
              {product && (
                <div className="product">
                  <pre>{JSON.stringify(product, null, 2)}</pre>
                </div>
              )}
            </div>
          )}
          {view === 'categories' && (
            <div>
              <button onClick={fetchCategories}>Load Categories</button>
              <pre>{JSON.stringify(categories, null, 2)}</pre>
            </div>
          )}
          {view === 'brands' && (
            <div>
              <button onClick={fetchBrands}>Load Brands</button>
              <pre>{JSON.stringify(brands, null, 2)}</pre>
            </div>
          )}
          {view === 'cart' && (
            <div>
              <button onClick={fetchCart}>Load Cart</button>
              <pre>{JSON.stringify(cartItems, null, 2)}</pre>
            </div>
          )}
          {view === 'orders' && (
            <div>
              <button onClick={fetchOrders}>Load Orders</button>
              <pre>{JSON.stringify(orders, null, 2)}</pre>
            </div>
          )}
        </div>
      ) : (
        <form onSubmit={login}>
          <div>
            <input value={email} onChange={e => setEmail(e.target.value)} placeholder="Email" />
          </div>
          <div>
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} placeholder="Password" />
          </div>
          <button type="submit">Login</button>
        </form>
      )}
      {error && <p style={{color: 'red'}}>{error}</p>}
    </div>
  );
}

ReactDOM.render(<App />, document.getElementById('root'));
