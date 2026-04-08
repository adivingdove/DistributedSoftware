var API_BASE = '/api';

function toast(msg, type) {
    var container = document.getElementById('toast-container');
    var el = document.createElement('div');
    el.className = 'toast toast-' + (type || 'info');
    el.textContent = msg;
    container.appendChild(el);
    setTimeout(function() { el.remove(); }, 3000);
}

function statusBadge(status) {
    var map = {
        0: { text: '待支付', cls: 'status-pending' },
        1: { text: '已支付', cls: 'status-paid' },
        2: { text: '已取消', cls: 'status-cancelled' },
        3: { text: '支付中', cls: 'status-paying' }
    };
    var s = map[status] || { text: '未知', cls: 'status-pending' };
    return '<span class="status-badge ' + s.cls + '">' + s.text + '</span>';
}

document.addEventListener('DOMContentLoaded', function() {
    checkLoginStatus();
    loadProducts();
    loadOrders();
    loadShardingOrders();

    var tabLogin = document.getElementById('tab-login');
    var tabRegister = document.getElementById('tab-register');
    var loginForm = document.getElementById('login-form');
    var registerForm = document.getElementById('register-form');

    if (tabLogin) tabLogin.addEventListener('click', function() { switchTab('login'); });
    if (tabRegister) tabRegister.addEventListener('click', function() { switchTab('register'); });
    if (loginForm) loginForm.addEventListener('submit', function(e) { e.preventDefault(); handleLogin(); });
    if (registerForm) registerForm.addEventListener('submit', function(e) { e.preventDefault(); handleRegister(); });

    document.getElementById('btn-check-server').addEventListener('click', checkServer);
    document.getElementById('btn-search').addEventListener('click', searchProducts);
    document.getElementById('search-input').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') searchProducts();
    });
});

function switchTab(tab) {
    var tabLogin = document.getElementById('tab-login');
    var tabRegister = document.getElementById('tab-register');
    var loginForm = document.getElementById('login-form');
    var registerForm = document.getElementById('register-form');
    var msg = document.getElementById('auth-msg');
    if (!tabLogin) return;
    if (msg) msg.textContent = '';
    if (tab === 'login') {
        tabLogin.classList.add('active'); tabRegister.classList.remove('active');
        loginForm.style.display = 'block'; registerForm.style.display = 'none';
    } else {
        tabLogin.classList.remove('active'); tabRegister.classList.add('active');
        loginForm.style.display = 'none'; registerForm.style.display = 'block';
    }
}

function showMessage(text, type) {
    var msg = document.getElementById('auth-msg');
    if (!msg) return;
    msg.className = 'message ' + type;
    msg.textContent = text;
}

function handleLogin() {
    var username = document.getElementById('login-username').value.trim();
    var password = document.getElementById('login-password').value.trim();
    if (!username || !password) { showMessage('请填写用户名和密码', 'error'); return; }

    fetch(API_BASE + '/user/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            localStorage.setItem('token', data.data.token);
            localStorage.setItem('username', data.data.username);
            localStorage.setItem('userId', String(data.data.userId));
            toast('登录成功，欢迎 ' + data.data.username, 'success');
            setTimeout(function() { location.reload(); }, 600);
        } else {
            showMessage(data.message || '登录失败', 'error');
        }
    })
    .catch(function() { showMessage('网络错误', 'error'); });
}

function handleRegister() {
    var username = document.getElementById('reg-username').value.trim();
    var password = document.getElementById('reg-password').value.trim();
    var phone = document.getElementById('reg-phone').value.trim();
    if (!username || !password || !phone) { showMessage('请填写所有字段', 'error'); return; }

    fetch(API_BASE + '/user/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password, phone: phone })
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            showMessage('注册成功！请切换到登录', 'success');
            document.getElementById('reg-username').value = '';
            document.getElementById('reg-password').value = '';
            document.getElementById('reg-phone').value = '';
            setTimeout(function() { switchTab('login'); }, 800);
        } else {
            showMessage(data.message || '注册失败', 'error');
        }
    })
    .catch(function() { showMessage('网络错误', 'error'); });
}

function checkLoginStatus() {
    var username = localStorage.getItem('username');
    var info = document.getElementById('user-info');
    var authSection = document.getElementById('auth-section');
    if (username) {
        info.innerHTML = '👤 ' + username + '  <a href="#" id="logout-link" style="color:rgba(255,255,255,0.85);text-decoration:none;margin-left:8px;font-size:13px;">退出登录</a>';
        document.getElementById('logout-link').addEventListener('click', function(e) { e.preventDefault(); logout(); });
        if (authSection) authSection.style.display = 'none';
    } else {
        info.textContent = '';
        if (authSection) authSection.style.display = '';
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('userId');
    location.reload();
}

function loadProducts() {
    fetch(API_BASE + '/product/list')
    .then(function(res) { return res.json(); })
    .then(function(data) {
        var container = document.getElementById('product-list');
        if (data.code === 200 && data.data && data.data.length > 0) {
            container.innerHTML = data.data.map(function(p) {
                var stockColor = p.stock > 0 ? '#10b981' : '#ef4444';
                var stockText = p.stock > 0 ? '库存 ' + p.stock + ' 件' : '已售罄';
                var btnDisabled = p.stock <= 0 ? ' disabled style="opacity:0.5;cursor:not-allowed;background:#9ca3af;"' : '';
                return '<div class="product-card">' +
                    '<h3>' + p.name + '</h3>' +
                    '<p class="description">' + (p.description || '') + '</p>' +
                    '<p class="price">\u00a5' + p.price + '</p>' +
                    '<p class="stock" style="color:' + stockColor + '">' + stockText + '</p>' +
                    '<button class="btn-seckill"' + btnDisabled + ' onclick="doSeckill(' + p.id + ')">⚡ 立即秒杀</button>' +
                '</div>';
            }).join('');
        } else {
            container.innerHTML = '<div class="empty-state"><div class="icon">📦</div><p>暂无商品</p></div>';
        }
    })
    .catch(function() {
        document.getElementById('product-list').innerHTML = '<div class="empty-state"><div class="icon">❌</div><p>加载商品失败</p></div>';
    });
}

function checkServer() {
    fetch(API_BASE + '/product/port')
    .then(function(res) { return res.json(); })
    .then(function(data) {
        document.getElementById('server-port').textContent = data.data || '未知';
        toast('当前负载均衡到端口 ' + data.data, 'info');
    })
    .catch(function() { document.getElementById('server-port').textContent = '请求失败'; });
}

function searchProducts() {
    var keyword = document.getElementById('search-input').value.trim();
    var container = document.getElementById('search-results');
    if (!keyword) { container.innerHTML = '<p style="color:#6b7280;font-size:14px;">请输入搜索关键词</p>'; return; }

    container.innerHTML = '<p style="color:#6b7280;">搜索中...</p>';
    fetch(API_BASE + '/search?keyword=' + encodeURIComponent(keyword))
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200 && data.data && data.data.length > 0) {
            container.innerHTML = '<div class="search-result-info">🔍 找到 ' + data.data.length + ' 条与「' + keyword + '」相关的结果（来自 ElasticSearch）</div>' +
                '<div class="product-grid">' + data.data.map(function(p) {
                    return '<div class="product-card" style="border-left:4px solid #3b82f6;">' +
                        '<h3>' + p.name + '</h3>' +
                        '<p class="description">' + (p.description || '') + '</p>' +
                        '<p class="price">\u00a5' + p.price + '</p>' +
                        '<p class="stock">库存 ' + p.stock + ' 件</p>' +
                    '</div>';
                }).join('') + '</div>';
        } else if (data.code === 200) {
            container.innerHTML = '<div class="empty-state"><div class="icon">🔍</div><p>未找到与「' + keyword + '」相关的商品</p></div>';
        } else {
            container.innerHTML = '<p style="color:#ef4444;">' + (data.message || '搜索失败') + '</p>';
        }
    })
    .catch(function() { container.innerHTML = '<p style="color:#ef4444;">搜索请求失败</p>'; });
}

function doSeckill(productId) {
    var userId = localStorage.getItem('userId');
    if (!userId) { toast('请先登录后再参与秒杀', 'warning'); return; }

    fetch(API_BASE + '/seckill/' + productId + '?userId=' + userId, { method: 'POST' })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            toast('秒杀成功！订单号: ' + data.data.orderId, 'success');
            loadProducts();
            setTimeout(function() { loadOrders(); loadShardingOrders(); }, 1500);
        } else {
            toast(data.message || '秒杀失败', 'error');
        }
    })
    .catch(function() { toast('网络错误', 'error'); });
}

var currentOrderPage = 1;
var orderPageSize = 5;

function loadOrders(page) {
    var userId = localStorage.getItem('userId');
    if (!userId) return;
    var container = document.getElementById('order-list');
    if (!container) return;
    if (page) currentOrderPage = page;

    fetch(API_BASE + '/seckill/orders?userId=' + userId + '&page=' + currentOrderPage + '&size=' + orderPageSize)
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200 && data.data && data.data.records && data.data.records.length > 0) {
            var pageData = data.data;
            var tryingOrders = pageData.records.filter(function(o) { return o.status === 3 && !pendingTxIds[o.id]; });
            var fetchPromises = tryingOrders.map(function(o) {
                return fetch(API_BASE + '/payment/tx?orderId=' + o.id)
                    .then(function(res) { return res.json(); })
                    .then(function(txData) {
                        if (txData.code === 200 && txData.data) {
                            pendingTxIds[o.id] = txData.data;
                        }
                    })
                    .catch(function() {});
            });
            Promise.all(fetchPromises).then(function() {
                renderOrders(container, pageData.records);
                renderPagination(container, pageData, 'loadOrders');
            });
        } else if (data.code === 200) {
            container.innerHTML = '<div class="empty-state"><div class="icon">📦</div><p>暂无订单</p></div>';
        }
    })
    .catch(function() { container.innerHTML = '<div class="empty-state"><div class="icon">❌</div><p>加载订单失败</p></div>'; });
}

var currentShardingPage = 1;

function loadShardingOrders(page) {
    var userId = localStorage.getItem('userId');
    if (!userId) return;
    var container = document.getElementById('sharding-order-list');
    if (!container) return;
    if (page) currentShardingPage = page;

    fetch(API_BASE + '/seckill/sharding/orders?userId=' + userId + '&page=' + currentShardingPage + '&size=' + orderPageSize)
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200 && data.data && data.data.records && data.data.records.length > 0) {
            var pageData = data.data;
            container.innerHTML = '<table class="order-table"><thead><tr>' +
                '<th>订单号</th><th>商品</th><th>价格</th><th>状态</th><th>下单时间</th><th>分片</th>' +
                '</tr></thead><tbody>' +
                pageData.records.map(function(o) {
                    var shard = 'ds' + (o.userId % 2);
                    return '<tr><td style="font-family:monospace;font-size:12px;">' + o.id + '</td><td>' + (o.productName || '') +
                        '</td><td style="font-weight:600;color:#f5576c;">\u00a5' + o.price + '</td><td>' + statusBadge(o.status) +
                        '</td><td style="color:#6b7280;font-size:12px;">' + (o.createTime || '') +
                        '</td><td><span class="status-badge" style="background:#ede9fe;color:#6d28d9;">' + shard + '</span></td></tr>';
                }).join('') + '</tbody></table>';
            renderPagination(container, pageData, 'loadShardingOrders');
        } else if (data.code === 200) {
            container.innerHTML = '<div class="empty-state"><div class="icon">📦</div><p>暂无分片订单</p></div>';
        }
    })
    .catch(function() { container.innerHTML = '<div class="empty-state"><div class="icon">❌</div><p>加载分片订单失败</p></div>'; });
}

var pendingTxIds = {};

function renderOrders(container, orders) {
    container.innerHTML = '<table class="order-table"><thead><tr>' +
        '<th>订单号</th><th>商品</th><th>价格</th><th>状态</th><th>支付倒计时</th><th>下单时间</th><th>操作</th>' +
        '</tr></thead><tbody>' +
        orders.map(function(o) {
            var actionBtn = '';
            var countdown = '';
            if (o.status === 0) {
                actionBtn = '<button class="btn btn-success btn-sm" onclick="payOrder(\'' + o.id + '\')">💳 支付</button> ' +
                            '<button class="btn btn-danger btn-sm" onclick="cancelOrder(\'' + o.id + '\')">✗ 取消</button>';
                if (o.expireTime) {
                    countdown = '<span class="countdown" data-expire="' + o.expireTime + '"></span>';
                }
            } else if (o.status === 3) {
                actionBtn = '<button class="btn btn-success btn-sm" onclick="confirmPayOrder(\'' + o.id + '\')">✓ 确认</button> ' +
                            '<button class="btn btn-danger btn-sm" onclick="cancelPayOrder(\'' + o.id + '\')">✗ 取消</button>';
            }
            return '<tr><td style="font-family:monospace;font-size:12px;">' + o.id + '</td><td>' + (o.productName || '') +
                '</td><td style="font-weight:600;color:#f5576c;">\u00a5' + o.price + '</td><td>' + statusBadge(o.status) +
                '</td><td>' + countdown + '</td><td style="color:#6b7280;font-size:12px;">' + (o.createTime || '') + '</td><td>' + actionBtn + '</td></tr>';
        }).join('') + '</tbody></table>';
    startCountdownTimers();
}

function payOrder(orderId) {
    var userId = localStorage.getItem('userId');
    if (!userId) { toast('请先登录', 'warning'); return; }

    fetch(API_BASE + '/payment/try?orderId=' + orderId + '&userId=' + userId, { method: 'POST' })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            pendingTxIds[orderId] = data.data.txId;
            toast('TCC-Try 成功，请确认或取消支付', 'info');
            loadOrders();
            loadShardingOrders();
            loadProducts();
        } else {
            toast(data.message || '支付预处理失败', 'error');
        }
    })
    .catch(function() { toast('网络错误', 'error'); });
}

function confirmPayOrder(orderId) {
    var txId = pendingTxIds[orderId];
    if (!txId) { toast('事务信息丢失，请刷新重试', 'warning'); return; }

    fetch(API_BASE + '/payment/confirm?txId=' + txId, { method: 'POST' })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            delete pendingTxIds[orderId];
            toast('TCC-Confirm 支付成功！', 'success');
            loadOrders();
            loadShardingOrders();
            loadProducts();
        } else {
            toast(data.message || '确认失败', 'error');
        }
    })
    .catch(function() { toast('网络错误', 'error'); });
}

function cancelPayOrder(orderId) {
    var txId = pendingTxIds[orderId];
    if (!txId) { toast('事务信息丢失，请刷新重试', 'warning'); return; }

    fetch(API_BASE + '/payment/cancel?txId=' + txId, { method: 'POST' })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            delete pendingTxIds[orderId];
            toast('TCC-Cancel 已取消，订单恢复待支付', 'info');
            loadOrders();
            loadShardingOrders();
            loadProducts();
        } else {
            toast(data.message || '取消失败', 'error');
        }
    })
    .catch(function() { toast('网络错误', 'error'); });
}

function cancelOrder(orderId) {
    var userId = localStorage.getItem('userId');
    if (!userId) { toast('请先登录', 'warning'); return; }

    fetch(API_BASE + '/seckill/cancel?orderId=' + orderId + '&userId=' + userId, { method: 'POST' })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.code === 200) {
            toast('订单已取消', 'success');
            loadOrders();
            loadShardingOrders();
            loadProducts();
        } else {
            toast(data.message || '取消失败', 'error');
        }
    })
    .catch(function() { toast('网络错误', 'error'); });
}

function renderPagination(container, pageData, loadFn) {
    if (pageData.totalPages <= 1) return;
    var nav = document.createElement('div');
    nav.className = 'pagination';
    nav.style.cssText = 'display:flex;justify-content:center;align-items:center;gap:6px;margin-top:12px;flex-wrap:wrap;';
    var html = '<span style="color:#6b7280;font-size:13px;margin-right:8px;">共 ' + pageData.total + ' 条</span>';
    if (pageData.page > 1) {
        html += '<button class="btn btn-sm" onclick="' + loadFn + '(' + (pageData.page - 1) + ')" style="padding:4px 10px;font-size:12px;cursor:pointer;">‹ 上一页</button>';
    }
    var start = Math.max(1, pageData.page - 2);
    var end = Math.min(pageData.totalPages, pageData.page + 2);
    for (var i = start; i <= end; i++) {
        if (i === pageData.page) {
            html += '<button class="btn btn-sm" style="padding:4px 10px;font-size:12px;background:#3b82f6;color:#fff;border:none;border-radius:4px;" disabled>' + i + '</button>';
        } else {
            html += '<button class="btn btn-sm" onclick="' + loadFn + '(' + i + ')" style="padding:4px 10px;font-size:12px;cursor:pointer;border:1px solid #d1d5db;border-radius:4px;background:#fff;">' + i + '</button>';
        }
    }
    if (pageData.page < pageData.totalPages) {
        html += '<button class="btn btn-sm" onclick="' + loadFn + '(' + (pageData.page + 1) + ')" style="padding:4px 10px;font-size:12px;cursor:pointer;">下一页 ›</button>';
    }
    nav.innerHTML = html;
    container.appendChild(nav);
}

var countdownTimer = null;

function startCountdownTimers() {
    if (countdownTimer) clearInterval(countdownTimer);
    countdownTimer = setInterval(function() {
        var spans = document.querySelectorAll('.countdown');
        if (spans.length === 0) { clearInterval(countdownTimer); countdownTimer = null; return; }
        var now = Date.now();
        spans.forEach(function(span) {
            var expire = new Date(span.getAttribute('data-expire')).getTime();
            var diff = expire - now;
            if (diff <= 0) {
                span.innerHTML = '<span style="color:#ef4444;font-weight:600;">已超时</span>';
                clearInterval(countdownTimer);
                countdownTimer = null;
                setTimeout(function() { loadOrders(); loadShardingOrders(); loadProducts(); }, 2000);
            } else {
                var m = Math.floor(diff / 60000);
                var s = Math.floor((diff % 60000) / 1000);
                var color = m < 5 ? '#ef4444' : '#f59e0b';
                span.innerHTML = '<span style="color:' + color + ';font-weight:600;">' + m + '分' + (s < 10 ? '0' : '') + s + '秒</span>';
            }
        });
    }, 1000);
}
