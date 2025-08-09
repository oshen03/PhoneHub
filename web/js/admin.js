// Admin Panel JS - Modular, fetch-based, Notification integrated

document.addEventListener('DOMContentLoaded', adminOnloadFunctions);

function adminOnloadFunctions() {
    // Load default tab data (Dashboard)
    loadDashboard();
    // Tab switching: load data when tab is shown
    document.querySelectorAll('a[data-bs-toggle="tab"]').forEach(tab => {
        tab.addEventListener('shown.bs.tab', function (e) {
            const target = e.target.getAttribute('href');
            if (target === '#dashboard') loadDashboard();
            else if (target === '#products') loadProducts();
            else if (target === '#orders') loadOrders();
            else if (target === '#customers') loadCustomers();
            else if (target === '#reports') loadReports();
            else if (target === '#settings') loadSettings();
        });
    });
    // Form submit listeners
    document.getElementById('productForm').addEventListener('submit', handleProductFormSubmit);
    // Add more form listeners as needed
}

// DASHBOARD
async function loadDashboard() {
    const popup = new Notification();
    try {
        const response = await fetch('DashboardServlet');
        if (response.ok) {
            const json = await response.json();
            if (json.status) {
                // Update summary statistics
                updateDashboardSummary(json.summary);
                // Update recent orders table
                updateRecentOrders(json.recentOrders.orders);
                // Update recent products table
                updateRecentProducts(json.recentProducts.products);
            } else {
                popup.error({ message: json.message || 'Failed to load dashboard data.' });
            }
        } else {
            popup.error({ message: 'Failed to load dashboard data.' });
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
    }
}

function updateDashboardSummary(summary) {
    // Update summary cards
    const totalOrdersElement = document.querySelector('#dashboard .stat-card:nth-child(1) h2');
    const totalProductsElement = document.querySelector('#dashboard .stat-card:nth-child(2) h2');
    const totalCustomersElement = document.querySelector('#dashboard .stat-card:nth-child(3) h2');
    const totalRevenueElement = document.querySelector('#dashboard .stat-card:nth-child(4) h2');
    
    if (totalOrdersElement) totalOrdersElement.textContent = summary.totalOrders || 0;
    if (totalProductsElement) totalProductsElement.textContent = summary.totalProducts || 0;
    if (totalCustomersElement) totalCustomersElement.textContent = summary.totalCustomers || 0;
    if (totalRevenueElement) totalRevenueElement.textContent = `$${summary.totalRevenue ? summary.totalRevenue.toFixed(2) : '0.00'}`;
    
    // Update low stock warning
    const lowStockElement = document.querySelector('#dashboard .stat-card:nth-child(2) span');
    if (lowStockElement && summary.lowStockCount > 0) {
        lowStockElement.textContent = `↓ ${summary.lowStockCount} low stock`;
        lowStockElement.className = 'text-danger';
    }
}

function updateRecentOrders(orders) {
    const tableBody = document.querySelector('#dashboard .card:nth-child(2) tbody');
    if (!tableBody) return;
    
    tableBody.innerHTML = '';
    if (orders && orders.length > 0) {
        orders.forEach(order => {
            const orderDate = new Date(order.createdAt).toLocaleDateString();
            const orderAmount = calculateOrderTotal(order); // You'll need to implement this
            
            tableBody.innerHTML += `
                <tr>
                    <td>#ORD-${order.id.toString().padStart(4, '0')}</td>
                    <td>${order.user ? order.user.first_name + ' ' + order.user.last_name : 'N/A'}</td>
                    <td>${orderDate}</td>
                    <td>$${orderAmount.toFixed(2)}</td>
                    <td><span class="badge bg-warning">Processing</span></td>
                    <td><a href="#" class="btn btn-sm btn-outline-primary" onclick="viewOrder(${order.id})">View</a></td>
                </tr>
            `;
        });
    } else {
        tableBody.innerHTML = '<tr><td colspan="6">No recent orders.</td></tr>';
    }
}

function updateRecentProducts(products) {
    const tableBody = document.querySelector('#dashboard .card:nth-child(3) tbody');
    if (!tableBody) return;
    
    tableBody.innerHTML = '';
    if (products && products.length > 0) {
        products.forEach(product => {
            const statusClass = product.qty > 0 ? 'bg-success' : 'bg-danger';
            const statusText = product.qty > 0 ? 'Active' : 'Inactive';
            
            tableBody.innerHTML += `
                <tr>
                    <td>P${product.id.toString().padStart(3, '0')}</td>
                    <td>${product.title}</td>
                    <td>${product.model ? product.model.brand ? product.model.brand.name : 'N/A' : 'N/A'}</td>
                    <td>$${product.price}</td>
                    <td>${product.qty}</td>
                    <td><span class="badge ${statusClass}">${statusText}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary me-1" onclick="editProduct(${product.id})">Edit</button>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteProduct(${product.id})">Delete</button>
                    </td>
                </tr>
            `;
        });
    } else {
        tableBody.innerHTML = '<tr><td colspan="7">No recent products.</td></tr>';
    }
}

function calculateOrderTotal(order) {
    // This is a placeholder - you might need to calculate from order items
    // For now, return a default value
    return 150.00;
}

// PRODUCTS
async function loadProducts() {
    const popup = new Notification();
    const tableBody = document.querySelector('#admin-product-table tbody');
    tableBody.innerHTML = '<tr><td colspan="7">Loading...</td></tr>';
    try {
        const response = await fetch('ProductServlet?action=list');
        if (response.ok) {
            const json = await response.json();
            if (json.status) {
                tableBody.innerHTML = '';
                json.productList.forEach(product => {
                    tableBody.innerHTML += `
                        <tr>
                            <td>${product.id}</td>
                            <td>${product.title}</td>
                            <td>${product.model && product.model.brand && product.model.brand.name ? product.model.brand.name : 'N/A'}</td>
                            <td>${product.price}</td>
                            <td>
                                <button class="btn btn-sm btn-outline-primary me-1" onclick="editProduct(${product.id})">Edit</button>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteProduct(${product.id})">Delete</button>
                            </td>
                        </tr>
                    `;
                });
            } else {
                tableBody.innerHTML = '<tr><td colspan="7">No products found.</td></tr>';
            }
        } else {
            popup.error({ message: 'Failed to load products.' });
            tableBody.innerHTML = '<tr><td colspan="7">Error loading products.</td></tr>';
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
        tableBody.innerHTML = '<tr><td colspan="7">Error loading products.</td></tr>';
    }
}

async function handleProductFormSubmit(e) {
    e.preventDefault();
    const popup = new Notification();
    const form = e.target;
    const data = {
        title: form.productTitle.value,
        brand: form.productBrand.value,
        price: form.productPrice.value,
        status: form.productStatus.value
    };
    try {
        const response = await fetch('ProductServlet?action=add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const json = await response.json();
        if (json.status) {
            popup.success({ message: 'Product added successfully!' });
            form.reset();
            var modal = bootstrap.Modal.getInstance(document.getElementById('addProductModal'));
            modal.hide();
            loadProducts();
        } else {
            popup.error({ message: json.message || 'Failed to add product.' });
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
    }
}

function editProduct(id) {
    // TODO: Load product data, fill modal, show modal for editing
}

async function deleteProduct(id) {
    const popup = new Notification();
    if (!confirm('Are you sure you want to delete this product?')) return;
    try {
        const response = await fetch(`ProductServlet?action=delete&id=${id}`, { method: 'DELETE' });
        const json = await response.json();
        if (json.status) {
            popup.success({ message: 'Product deleted.' });
            loadProducts();
        } else {
            popup.error({ message: json.message || 'Failed to delete product.' });
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
    }
}

// ORDERS
async function loadOrders() {
    const popup = new Notification();
    const tableBody = document.querySelector('#admin-order-table tbody');
    tableBody.innerHTML = '<tr><td colspan="6">Loading...</td></tr>';
    try {
        const response = await fetch('OrderServlet?action=list');
        if (response.ok) {
            const json = await response.json();
            if (json.status) {
                tableBody.innerHTML = '';
                json.orderList.forEach(order => {
                    tableBody.innerHTML += `
                        <tr>
                            <td>${order.id}</td>
                            <td>${order.customerName}</td>
                            <td>${order.date}</td>
                            <td>${order.amount}</td>
                            <td><span class="badge ${order.status === 'Delivered' ? 'bg-success' : order.status === 'Cancelled' ? 'bg-danger' : 'bg-warning'}">${order.status}</span></td>
                            <td>
                                <button class="btn btn-sm btn-outline-primary me-1" onclick="viewOrder(${order.id})">View</button>
                            </td>
                        </tr>
                    `;
                });
            } else {
                tableBody.innerHTML = '<tr><td colspan="6">No orders found.</td></tr>';
            }
        } else {
            popup.error({ message: 'Failed to load orders.' });
            tableBody.innerHTML = '<tr><td colspan="6">Error loading orders.</td></tr>';
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
        tableBody.innerHTML = '<tr><td colspan="6">Error loading orders.</td></tr>';
    }
}

async function viewOrder(id) {
    const popup = new Notification();
    const modalBody = document.getElementById('orderDetailsBody');
    modalBody.innerHTML = 'Loading...';
    try {
        const response = await fetch(`OrderServlet?action=view&id=${id}`);
        const json = await response.json();
        if (json.status) {
            // Render order details and status update form
            modalBody.innerHTML = `
                <div><strong>Order ID:</strong> ${json.order.id}</div>
                <div><strong>Customer:</strong> ${json.order.customerName}</div>
                <div><strong>Date:</strong> ${json.order.date}</div>
                <div><strong>Amount:</strong> ${json.order.amount}</div>
                <div><strong>Status:</strong> 
                    <select id="orderStatusSelect" class="form-select w-auto d-inline-block ms-2">
                        <option value="Processing" ${json.order.status === 'Processing' ? 'selected' : ''}>Processing</option>
                        <option value="Delivered" ${json.order.status === 'Delivered' ? 'selected' : ''}>Delivered</option>
                        <option value="Cancelled" ${json.order.status === 'Cancelled' ? 'selected' : ''}>Cancelled</option>
                    </select>
                    <button class="btn btn-sm btn-orange ms-2" onclick="updateOrderStatus(${json.order.id})">Update</button>
                </div>
                <hr/>
                <div><strong>Items:</strong></div>
                <ul>
                    ${json.order.items.map(item => `<li>${item.productTitle} x${item.qty} - ${item.price}</li>`).join('')}
                </ul>
            `;
            var modal = new bootstrap.Modal(document.getElementById('viewOrderModal'));
            modal.show();
        } else {
            modalBody.innerHTML = 'Order not found.';
        }
    } catch (err) {
        modalBody.innerHTML = 'Error loading order.';
        popup.error({ message: 'Error: ' + err.message });
    }
}

async function updateOrderStatus(orderId) {
    const popup = new Notification();
    const status = document.getElementById('orderStatusSelect').value;
    try {
        const response = await fetch('OrderServlet?action=updateStatus', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: orderId, status })
        });
        const json = await response.json();
        if (json.status) {
            popup.success({ message: 'Order status updated.' });
            loadOrders();
            var modal = bootstrap.Modal.getInstance(document.getElementById('viewOrderModal'));
            modal.hide();
        } else {
            popup.error({ message: json.message || 'Failed to update order.' });
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
    }
}

// CUSTOMERS
async function loadCustomers() {
    const popup = new Notification();
    const tableBody = document.querySelector('#admin-customer-table tbody');
    tableBody.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
    try {
        const response = await fetch('CustomerServlet?action=list');
        if (response.ok) {
            const json = await response.json();
            if (json.status) {
                tableBody.innerHTML = '';
                json.customerList.forEach(customer => {
                    tableBody.innerHTML += `
                        <tr>
                            <td>${customer.id}</td>
                            <td>${customer.name}</td>
                            <td>${customer.email}</td>
                            <td><span class="badge ${customer.status === 'active' ? 'bg-success' : 'bg-danger'}">${customer.status}</span></td>
                            <td>
                                <button class="btn btn-sm btn-outline-primary me-1" onclick="viewCustomer(${customer.id})">View</button>
                                <button class="btn btn-sm btn-outline-${customer.status === 'active' ? 'danger' : 'success'}" onclick="toggleCustomerStatus(${customer.id}, '${customer.status}')">${customer.status === 'active' ? 'Block' : 'Unblock'}</button>
                            </td>
                        </tr>
                    `;
                });
            } else {
                tableBody.innerHTML = '<tr><td colspan="5">No customers found.</td></tr>';
            }
        } else {
            popup.error({ message: 'Failed to load customers.' });
            tableBody.innerHTML = '<tr><td colspan="5">Error loading customers.</td></tr>';
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
        tableBody.innerHTML = '<tr><td colspan="5">Error loading customers.</td></tr>';
    }
}

async function viewCustomer(id) {
    const popup = new Notification();
    const modalBody = document.getElementById('customerDetailsBody');
    modalBody.innerHTML = 'Loading...';
    try {
        const response = await fetch(`CustomerServlet?action=view&id=${id}`);
        const json = await response.json();
        if (json.status) {
            modalBody.innerHTML = `
                <div><strong>ID:</strong> ${json.customer.id}</div>
                <div><strong>Name:</strong> ${json.customer.name}</div>
                <div><strong>Email:</strong> ${json.customer.email}</div>
                <div><strong>Status:</strong> <span class="badge ${json.customer.status === 'active' ? 'bg-success' : 'bg-danger'}">${json.customer.status}</span></div>
                <div><strong>Registered:</strong> ${json.customer.registered}</div>
                <div><strong>Orders:</strong> ${json.customer.orderCount}</div>
            `;
            var modal = new bootstrap.Modal(document.getElementById('viewCustomerModal'));
            modal.show();
        } else {
            modalBody.innerHTML = 'Customer not found.';
        }
    } catch (err) {
        modalBody.innerHTML = 'Error loading customer.';
        popup.error({ message: 'Error: ' + err.message });
    }
}

async function toggleCustomerStatus(id, currentStatus) {
    const popup = new Notification();
    const action = currentStatus === 'active' ? 'block' : 'unblock';
    try {
        const response = await fetch(`CustomerServlet?action=${action}&id=${id}`, { method: 'POST' });
        const json = await response.json();
        if (json.status) {
            popup.success({ message: `Customer ${action === 'block' ? 'blocked' : 'unblocked'}.` });
            loadCustomers();
        } else {
            popup.error({ message: json.message || 'Failed to update customer.' });
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
    }
}

// REPORTS
async function loadReports() {
    const popup = new Notification();
    const reportsContent = document.getElementById('admin-reports-content');
    reportsContent.innerHTML = 'Loading...';
    try {
        const response = await fetch('ReportServlet?action=summary');
        const json = await response.json();
        if (json.status) {
            // Example: Render sales/stock summary as a table
            reportsContent.innerHTML = `
                <h5>Sales Summary</h5>
                <table class="table table-bordered">
                    <tr><th>Total Sales</th><td>${json.salesSummary.totalSales}</td></tr>
                    <tr><th>Total Orders</th><td>${json.salesSummary.totalOrders}</td></tr>
                    <tr><th>Total Customers</th><td>${json.salesSummary.totalCustomers}</td></tr>
                </table>
            `;
        } else {
            reportsContent.innerHTML = 'No report data.';
        }
    } catch (err) {
        reportsContent.innerHTML = 'Error loading reports.';
        popup.error({ message: 'Error: ' + err.message });
    }
}

// SETTINGS
async function loadSettings() {
    const popup = new Notification();
    const nameInput = document.getElementById('adminName');
    const emailInput = document.getElementById('adminEmail');
    // Password left blank for security
    try {
        const response = await fetch('AdminSettingsServlet?action=profile');
        const json = await response.json();
        if (json.status) {
            nameInput.value = json.admin.name;
            emailInput.value = json.admin.email;
        } else {
            popup.error({ message: json.message || 'Failed to load profile.' });
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
    }
}

document.getElementById('admin-settings-form').addEventListener('submit', handleSettingsFormSubmit);

async function handleSettingsFormSubmit(e) {
    e.preventDefault();
    const popup = new Notification();
    const form = e.target;
    const data = {
        name: form.adminName.value,
        email: form.adminEmail.value,
        password: form.adminPassword.value
    };
    try {
        const response = await fetch('AdminSettingsServlet?action=update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        const json = await response.json();
        if (json.status) {
            popup.success({ message: 'Profile updated.' });
            form.adminPassword.value = '';
        } else {
            popup.error({ message: json.message || 'Failed to update profile.' });
        }
    } catch (err) {
        popup.error({ message: 'Error: ' + err.message });
    }
}
