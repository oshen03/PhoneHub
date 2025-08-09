// Enhanced Admin Panel JS - Complete with Entity Management & Reports

document.addEventListener('DOMContentLoaded', adminOnloadFunctions);

function adminOnloadFunctions() {
    // Load default tab data (Dashboard)
    loadDashboard();

    // Tab switching: load data when tab is shown
    document.querySelectorAll('a[data-bs-toggle="tab"]').forEach(tab => {
        tab.addEventListener('shown.bs.tab', function (e) {
            const target = e.target.getAttribute('href');
            if (target === '#dashboard')
                loadDashboard();
            else if (target === '#products')
                loadProducts();
            else if (target === '#orders')
                loadOrders();
            else if (target === '#customers')
                loadCustomers();
            else if (target === '#reports')
                loadReports();
            else if (target === '#entities')
                loadEntityManagement();
            else if (target === '#settings')
                loadSettings();
        });
    });

    // Form submit listeners
    const productForm = document.getElementById('productForm');
    if (productForm) {
        productForm.addEventListener('submit', handleProductFormSubmit);
    }

    const settingsForm = document.getElementById('admin-settings-form');
    if (settingsForm) {
        settingsForm.addEventListener('submit', handleSettingsFormSubmit);
    }

    const entityForm = document.getElementById('entityForm');
    if (entityForm) {
        entityForm.addEventListener('submit', handleEntityFormSubmit);
    }
}

// DASHBOARD
async function loadDashboard() {
    const popup = new Notification();
    showLoading('dashboard');

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
                // Update dashboard metrics
                updateDashboardMetrics(json.metrics);
            } else {
                popup.error({message: json.message || 'Failed to load dashboard data.'});
            }
        } else {
            popup.error({message: 'Failed to load dashboard data. Server error.'});
        }
    } catch (err) {
        popup.error({message: 'Network error: ' + err.message});
    } finally {
        hideLoading('dashboard');
    }
}

function updateDashboardSummary(summary) {
    // Update summary cards with proper selectors
    const totalOrdersElement = document.querySelector('#totalOrders');
    const totalProductsElement = document.querySelector('#totalProducts');
    const totalCustomersElement = document.querySelector('#totalCustomers');
    const totalRevenueElement = document.querySelector('#totalRevenue');

    if (totalOrdersElement)
        totalOrdersElement.textContent = summary.totalOrders || 0;
    if (totalProductsElement)
        totalProductsElement.textContent = summary.totalProducts || 0;
    if (totalCustomersElement)
        totalCustomersElement.textContent = summary.totalCustomers || 0;
    if (totalRevenueElement)
        totalRevenueElement.textContent = `Lkr ${(summary.totalRevenue || 0).toFixed(2)}`;
}

function updateRecentOrders(orders) {
    const tableBody = document.querySelector('#recentOrdersTable');
    if (!tableBody)
        return;

    tableBody.innerHTML = '';
    if (orders && orders.length > 0) {
        orders.forEach(order => {
            const orderDate = new Date(order.createdAt).toLocaleDateString();
            const customerName = order.user ?
                    `${order.user.first_name} ${order.user.last_name}` : 'Guest';

            tableBody.innerHTML += `
                <tr>
                    <td>#${order.id.toString().padStart(4, '0')}</td>
                    <td>${customerName}</td>
                    <td>${orderDate}</td>
                    <td>Lkr ${(order.total || 0).toFixed(2)}</td>
                    <td><span class="badge bg-warning">${order.status || 'Processing'}</span></td>
                    <td><button class="btn btn-sm btn-outline-primary" onclick="viewOrder(${order.id})">View</button></td>
                </tr>
            `;
        });
    } else {
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center">No recent orders found</td></tr>';
    }
}

function updateRecentProducts(products) {
    const tableBody = document.querySelector('#recentProductsTable');
    if (!tableBody)
        return;

    tableBody.innerHTML = '';
    if (products && products.length > 0) {
        products.forEach(product => {
            const statusClass = product.qty > 0 ? 'bg-success' : 'bg-danger';
            const statusText = product.stockStatus || (product.qty > 0 ? 'In Stock' : 'Out of Stock');
            const brandName = product.model?.brand?.name || 'N/A';

            tableBody.innerHTML += `
                <tr>
                    <td>P${product.id.toString().padStart(3, '0')}</td>
                    <td>${product.title}</td>
                    <td>${brandName}</td>
                    <td>Lkr ${product.price}</td>
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
        tableBody.innerHTML = '<tr><td colspan="7" class="text-center">No recent products found</td></tr>';
    }
}

function updateDashboardMetrics(metrics) {
    if (!metrics)
        return;

    // Update top product
    const topProductElement = document.querySelector('#topProduct');
    if (topProductElement && metrics.topProduct) {
        topProductElement.innerHTML = `
            <strong>${metrics.topProduct.title}</strong><br>
            <small>${metrics.topProduct.soldCount} units sold this month</small>
        `;
    }

    // Update new customers
    const newCustomersElement = document.querySelector('#newCustomers');
    if (newCustomersElement) {
        newCustomersElement.textContent = metrics.newCustomers || 0;
    }

    // Update pending orders if element exists
    const pendingOrdersElement = document.querySelector('#pendingOrders');
    if (pendingOrdersElement) {
        pendingOrdersElement.textContent = metrics.pendingOrders || 0;
    }
}

// PRODUCTS
async function loadProducts() {
    const popup = new Notification();
    const tableBody = document.querySelector('#admin-product-table tbody');
    if (!tableBody)
        return;

    tableBody.innerHTML = '<tr><td colspan="6" class="text-center">Loading...</td></tr>';

    try {
        const response = await fetch('ProductServlet?action=list');
        if (response.ok) {
            const json = await response.json();
            if (json.status) {
                tableBody.innerHTML = '';
                json.productList.forEach(product => {
                    const brandName = product.model?.brand?.name || 'N/A';
                    const statusBadge = product.qty > 0 ?
                            '<span class="badge bg-success">In Stock</span>' :
                            '<span class="badge bg-danger">Out of Stock</span>';

                    tableBody.innerHTML += `
                        <tr>
                            <td>P${product.id.toString().padStart(3, '0')}</td>
                            <td>${product.title}</td>
                            <td>${brandName}</td>
                            <td>$${product.price}</td>
                            <td>${product.qty}</td>
                            <td>
                                <button class="btn btn-sm btn-outline-primary me-1" onclick="editProduct(${product.id})">Edit</button>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteProduct(${product.id})">Delete</button>
                            </td>
                        </tr>
                    `;
                });
            } else {
                tableBody.innerHTML = '<tr><td colspan="6" class="text-center">No products found</td></tr>';
            }
        } else {
            popup.error({message: 'Failed to load products.'});
            tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Error loading products</td></tr>';
        }
    } catch (err) {
        popup.error({message: 'Network error: ' + err.message});
        tableBody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Network error loading products</td></tr>';
    }
}

async function handleProductFormSubmit(e) {
    e.preventDefault();
    const popup = new Notification();
    const form = e.target;
    const data = {
        title: form.productTitle.value,
        brand: form.productBrand.value,
        price: parseFloat(form.productPrice.value),
        qty: parseInt(form.productQty.value) || 0,
        status: form.productStatus.value
    };

    try {
        const response = await fetch('ProductServlet?action=add', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        const json = await response.json();
        if (json.status) {
            popup.success({message: 'Product added successfully!'});
            form.reset();
            const modal = bootstrap.Modal.getInstance(document.getElementById('addProductModal'));
            if (modal)
                modal.hide();
            loadProducts();
        } else {
            popup.error({message: json.message || 'Failed to add product.'});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

function editProduct(id) {
    // TODO: Implement product editing functionality
    console.log('Edit product:', id);
}

async function deleteProduct(id) {
    const popup = new Notification();
    if (!confirm('Are you sure you want to delete this product?'))
        return;

    try {
        const response = await fetch(`ProductServlet?action=delete&id=${id}`, {
            method: 'DELETE'
        });
        const json = await response.json();
        if (json.status) {
            popup.success({message: 'Product deleted successfully.'});
            loadProducts();
        } else {
            popup.error({message: json.message || 'Failed to delete product.'});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

// ORDERS
async function loadOrders() {
    const popup = new Notification();
    const tableBody = document.querySelector('#admin-order-table tbody');
    if (!tableBody)
        return;

    tableBody.innerHTML = '<tr><td colspan="6" class="text-center">Loading...</td></tr>';

    try {
        const response = await fetch('OrderServlet?action=list');
        if (response.ok) {
            const json = await response.json();
            if (json.status) {
                tableBody.innerHTML = '';
                json.orderList.forEach(order => {
                    const statusClass = getStatusClass(order.status);
                    tableBody.innerHTML += `
                        <tr>
                            <td>#${order.id.toString().padStart(4, '0')}</td>
                            <td>${order.customerName}</td>
                            <td>${new Date(order.date).toLocaleDateString()}</td>
                            <td>Lkr ${parseFloat(order.amount).toFixed(2)}</td>
                            <td><span class="badge ${statusClass}">${order.status}</span></td>
                            <td>
                                <button class="btn btn-sm btn-outline-primary" onclick="viewOrder(${order.id})">View</button>
                            </td>
                        </tr>
                    `;
                });
            } else {
                tableBody.innerHTML = '<tr><td colspan="6" class="text-center">No orders found</td></tr>';
            }
        } else {
            popup.error({message: 'Failed to load orders.'});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

function getStatusClass(status) {
    switch (status?.toLowerCase()) {
        case 'delivered':
            return 'bg-success';
        case 'cancelled':
            return 'bg-danger';
        case 'processing':
            return 'bg-warning';
        case 'shipped':
            return 'bg-info';
        default:
            return 'bg-secondary';
    }
}

async function viewOrder(id) {
    const popup = new Notification();
    const modalBody = document.getElementById('orderDetailsBody');
    if (!modalBody)
        return;

    modalBody.innerHTML = 'Loading...';

    try {
        const response = await fetch(`OrderServlet?action=view&id=${id}`);
        const json = await response.json();
        if (json.status) {
            modalBody.innerHTML = `
                <div class="mb-3">
                    <h6>Order Information</h6>
                    <p><strong>Order ID:</strong> #${json.order.id.toString().padStart(4, '0')}</p>
                    <p><strong>Customer:</strong> ${json.order.customerName}</p>
                    <p><strong>Date:</strong> ${new Date(json.order.date).toLocaleDateString()}</p>
                    <p><strong>Amount:</strong> $${parseFloat(json.order.amount).toFixed(2)}</p>
                </div>
                <div class="mb-3">
                    <h6>Update Status</h6>
                    <div class="d-flex align-items-center">
                        <select id="orderStatusSelect" class="form-select me-2" style="width: auto;">
                            <option value="Processing" ${json.order.status === 'Processing' ? 'selected' : ''}>Processing</option>
                            <option value="Shipped" ${json.order.status === 'Shipped' ? 'selected' : ''}>Shipped</option>
                            <option value="Delivered" ${json.order.status === 'Delivered' ? 'selected' : ''}>Delivered</option>
                            <option value="Cancelled" ${json.order.status === 'Cancelled' ? 'selected' : ''}>Cancelled</option>
                        </select>
                        <button class="btn btn-warning" onclick="updateOrderStatus(${json.order.id})">Update</button>
                    </div>
                </div>
                <div>
                    <h6>Order Items</h6>
                    <div class="table-responsive">
                        <table class="table table-sm">
                            <thead>
                                <tr>
                                    <th>Product</th>
                                    <th>Qty</th>
                                    <th>Price</th>
                                    <th>Total</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${json.order.items.map(item => `
                                    <tr>
                                        <td>${item.productTitle}</td>
                                        <td>${item.qty}</td>
                                        <td>$${parseFloat(item.price).toFixed(2)}</td>
                                        <td>$${(item.qty * parseFloat(item.price)).toFixed(2)}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            `;
            const modal = new bootstrap.Modal(document.getElementById('viewOrderModal'));
            modal.show();
        } else {
            modalBody.innerHTML = '<div class="alert alert-danger">Order not found.</div>';
        }
    } catch (err) {
        modalBody.innerHTML = '<div class="alert alert-danger">Error loading order details.</div>';
        popup.error({message: 'Error: ' + err.message});
    }
}

async function updateOrderStatus(orderId) {
    const popup = new Notification();
    const status = document.getElementById('orderStatusSelect').value;

    try {
        const response = await fetch('OrderServlet?action=updateStatus', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({id: orderId, status})
        });
        const json = await response.json();
        if (json.status) {
            popup.success({message: 'Order status updated successfully.'});
            loadOrders();
            const modal = bootstrap.Modal.getInstance(document.getElementById('viewOrderModal'));
            if (modal)
                modal.hide();
        } else {
            popup.error({message: json.message || 'Failed to update order status.'});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

// CUSTOMERS
async function loadCustomers() {
    const popup = new Notification();
    const tableBody = document.querySelector('#admin-customer-table tbody');
    if (!tableBody)
        return;

    tableBody.innerHTML = '<tr><td colspan="5" class="text-center">Loading...</td></tr>';

    try {
        const response = await fetch('CustomerServlet?action=list');
        if (response.ok) {
            const json = await response.json();
            if (json.status) {
                tableBody.innerHTML = '';
                json.customerList.forEach(customer => {
                    const statusClass = customer.status === 'active' ? 'bg-success' : 'bg-danger';
                    const actionText = customer.status === 'active' ? 'Block' : 'Unblock';
                    const actionClass = customer.status === 'active' ? 'btn-outline-danger' : 'btn-outline-success';

                    tableBody.innerHTML += `
                        <tr>
                            <td>C${customer.id.toString().padStart(3, '0')}</td>
                            <td>${customer.name}</td>
                            <td>${customer.email}</td>
                            <td><span class="badge ${statusClass}">${customer.status}</span></td>
                            <td>
                                <button class="btn btn-sm btn-outline-primary me-1" onclick="viewCustomer(${customer.id})">View</button>
                                <button class="btn btn-sm ${actionClass}" onclick="toggleCustomerStatus(${customer.id}, '${customer.status}')">${actionText}</button>
                            </td>
                        </tr>
                    `;
                });
            } else {
                tableBody.innerHTML = '<tr><td colspan="5" class="text-center">No customers found</td></tr>';
            }
        } else {
            popup.error({message: 'Failed to load customers.'});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

async function viewCustomer(id) {
    const popup = new Notification();
    const modalBody = document.getElementById('customerDetailsBody');
    if (!modalBody)
        return;

    modalBody.innerHTML = 'Loading...';

    try {
        const response = await fetch(`CustomerServlet?action=view&id=${id}`);
        const json = await response.json();
        if (json.status) {
            const customer = json.customer;
            modalBody.innerHTML = `
                <div class="row">
                    <div class="col-md-6">
                        <h6>Customer Information</h6>
                        <p><strong>ID:</strong> C${customer.id.toString().padStart(3, '0')}</p>
                        <p><strong>Name:</strong> ${customer.name}</p>
                        <p><strong>Email:</strong> ${customer.email}</p>
                    </div>
                    <div class="col-md-6">
                        <h6>Account Details</h6>
                        <p><strong>Status:</strong> <span class="badge ${customer.status === 'active' ? 'bg-success' : 'bg-danger'}">${customer.status}</span></p>
                        <p><strong>Registered:</strong> ${new Date(customer.registered).toLocaleDateString()}</p>
                        <p><strong>Total Orders:</strong> ${customer.orderCount || 0}</p>
                    </div>
                </div>
            `;
            const modal = new bootstrap.Modal(document.getElementById('viewCustomerModal'));
            modal.show();
        } else {
            modalBody.innerHTML = '<div class="alert alert-danger">Customer not found.</div>';
        }
    } catch (err) {
        modalBody.innerHTML = '<div class="alert alert-danger">Error loading customer details.</div>';
        popup.error({message: 'Error: ' + err.message});
    }
}

async function toggleCustomerStatus(id, currentStatus) {
    const popup = new Notification();
    const action = currentStatus === 'active' ? 'block' : 'unblock';
    const confirmMessage = `Are you sure you want to ${action} this customer?`;

    if (!confirm(confirmMessage))
        return;

    try {
        const response = await fetch(`CustomerServlet?action=${action}&id=${id}`, {
            method: 'POST'
        });
        const json = await response.json();
        if (json.status) {
            popup.success({message: `Customer ${action}ed successfully.`});
            loadCustomers();
        } else {
            popup.error({message: json.message || `Failed to ${action} customer.`});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

// ENTITY MANAGEMENT
async function loadEntityManagement() {
    // Load all entity management sections
    loadEntityList('brand');
    loadEntityList('color');
    loadEntityList('storage');
    loadEntityList('quality');
    loadEntityList('city');
    loadEntityList('deliverytype');
    loadEntityList('model');
}

async function loadEntityList(entity) {
    const popup = new Notification();
    const container = document.getElementById(`${entity}sList`);
    if (!container)
        return;

    container.innerHTML = '<div class="text-center">Loading...</div>';

    try {
        const response = await fetch(`EntityManagementServlet?entity=${entity}&action=list`);
        if (response.ok) {
            const json = await response.json();
            if (json.status) {
                let html = '';
                if (json.entities.length > 0) {
                    json.entities.forEach(item => {
                        let displayText = '';
                        switch (entity) {
                            case 'brand':
                            case 'city':
                                displayText = item.name;
                                break;
                            case 'color':
                            case 'storage':
                            case 'quality':
                                displayText = item.value;
                                break;
                            case 'deliverytype':
                                displayText = `${item.name} - $${item.price}`;
                                break;
                            case 'model':
                                displayText = `${item.name} (${item.brandName || 'No Brand'})`;
                                break;
                        }

                        html += `
                            <div class="d-flex justify-content-between align-items-center mb-2 p-2 border rounded">
                                <span>${displayText}</span>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteEntity('${entity}', ${item.id})">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </div>
                        `;
                    });
                } else {
                    html = '<div class="text-muted">No items found</div>';
                }
                container.innerHTML = html;
            } else {
                container.innerHTML = '<div class="text-danger">Error loading data</div>';
                popup.error({message: json.message || 'Failed to load entity list.'});
            }
        }
    } catch (err) {
        container.innerHTML = '<div class="text-danger">Network error</div>';
        popup.error({message: 'Network error: ' + err.message});
    }
}

function showEntityModal(entity) {
    const modal = document.getElementById('entityModal');
    const modalTitle = document.getElementById('entityModalLabel');
    const formFields = document.getElementById('entityFormFields');

    modalTitle.textContent = `Add ${entity.charAt(0).toUpperCase() + entity.slice(1)}`;

    let fieldsHtml = '';
    switch (entity) {
        case 'brand':
        case 'city':
            fieldsHtml = `
                <div class="mb-3">
                    <label for="entityName" class="form-label">Name</label>
                    <input type="text" class="form-control" id="entityName" name="name" required>
                </div>
            `;
            break;
        case 'color':
        case 'storage':
        case 'quality':
            fieldsHtml = `
                <div class="mb-3">
                    <label for="entityValue" class="form-label">Value</label>
                    <input type="text" class="form-control" id="entityValue" name="value" required>
                </div>
            `;
            break;
        case 'deliverytype':
            fieldsHtml = `
                <div class="mb-3">
                    <label for="entityName" class="form-label">Name</label>
                    <input type="text" class="form-control" id="entityName" name="name" required>
                </div>
                <div class="mb-3">
                    <label for="entityPrice" class="form-label">Price</label>
                    <input type="number" step="0.01" class="form-control" id="entityPrice" name="price" required>
                </div>
            `;
            break;
        case 'model':
            fieldsHtml = `
                <div class="mb-3">
                    <label for="entityName" class="form-label">Model Name</label>
                    <input type="text" class="form-control" id="entityName" name="name" required>
                </div>
                <div class="mb-3">
                    <label for="entityBrandId" class="form-label">Brand</label>
                    <select class="form-select" id="entityBrandId" name="brandId" required>
                        <option value="">Select Brand</option>
                    </select>
                </div>
            `;
            break;
    }

    formFields.innerHTML = fieldsHtml;

    // Load brands for model entity
    if (entity === 'model') {
        loadBrandsForModelDropdown();
    }

    // Set entity type for form submission
    document.getElementById('entityForm').setAttribute('data-entity', entity);

    const bsModal = new bootstrap.Modal(modal);
    bsModal.show();
}

async function loadBrandsForModelDropdown() {
    const popup = new Notification();
    try {
        const response = await fetch('EntityManagementServlet?entity=brand&action=list');
        const json = await response.json();
        if (json.status) {
            const select = document.getElementById('entityBrandId');
            if (select) {
                select.innerHTML = '<option value="">Select Brand</option>';
                json.entities.forEach(brand => {
                    select.innerHTML += `<option value="${brand.id}">${brand.name}</option>`;
                });
            }
        } else {
            popup.error({message: json.message || 'Failed to load brands.'});
        }
    } catch (err) {
        popup.error({message: 'Error loading brands: ' + err.message});
    }
}

async function handleEntityFormSubmit(e) {
    e.preventDefault();
    const popup = new Notification();
    const form = e.target;
    const entity = form.getAttribute('data-entity');

    const formData = new FormData(form);
    const data = Object.fromEntries(formData);

    // Convert specific fields
    if (data.price)
        data.price = parseFloat(data.price);
    if (data.brandId)
        data.brandId = parseInt(data.brandId);

    try {
        const response = await fetch(`EntityManagementServlet?entity=${entity}&action=add`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        const json = await response.json();
        if (json.status) {
            popup.success({message: `${entity} added successfully!`});
            form.reset();
            const modal = bootstrap.Modal.getInstance(document.getElementById('entityModal'));
            if (modal)
                modal.hide();
            loadEntityList(entity);
        } else {
            popup.error({message: json.message || `Failed to add ${entity}.`});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

async function deleteEntity(entity, id) {
    const popup = new Notification();
    if (!confirm(`Are you sure you want to delete this ${entity}?`))
        return;

    try {
        const response = await fetch(`EntityManagementServlet?entity=${entity}&action=delete&id=${id}`, {
            method: 'DELETE'
        });
        const json = await response.json();
        if (json.status) {
            popup.success({message: `${entity} deleted successfully.`});
            loadEntityList(entity);
        } else {
            popup.error({message: json.message || `Failed to delete ${entity}.`});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

// REPORTS
async function loadReports() {
    loadReportSection('summary');
}

async function loadReportSection(section) {
    // Update active button
    document.querySelectorAll('.btn-group button').forEach(btn => {
        btn.classList.remove('active');
    });
    event?.target?.classList.add('active');

    const container = document.getElementById('reportsContent');
    if (!container)
        return;

    container.innerHTML = '<div class="text-center">Loading...</div>';

    let content = '';
    switch (section) {
        case 'summary':
            content = await loadSummaryReport();
            break;
        case 'sales':
            content = await loadSalesReport();
            break;
        case 'inventory':
            content = await loadInventoryReport();
            break;
        case 'customers':
            content = await loadCustomerReport();
            break;
        case 'brands':
            content = await loadBrandReport();
            break;
        default:
            content = '<div class="alert alert-warning">Report section not found.</div>';
    }

    container.innerHTML = content;
}

async function loadSummaryReport() {
    const popup = new Notification();
    try {
        const response = await fetch('ReportServlet?action=summary');
        const json = await response.json();
        if (json.status) {
            const summary = json.salesSummary;
            return `
                <div class="report-section">
                    <h5><i class="bi bi-bar-chart me-2"></i>Business Summary</h5>
                    <div class="row">
                        <div class="col-md-3">
                            <div class="stat-card p-3 text-center">
                                <h4 class="text-primary">${summary.totalSales.toFixed(2)}</h4>
                                <p class="mb-0">Total Sales Revenue</p>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="stat-card p-3 text-center">
                                <h4 class="text-success">${summary.totalOrders}</h4>
                                <p class="mb-0">Total Orders</p>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="stat-card p-3 text-center">
                                <h4 class="text-info">${summary.totalCustomers}</h4>
                                <p class="mb-0">Total Customers</p>
                            </div>
                        </div>
                        <div class="col-md-3">
                            <div class="stat-card p-3 text-center">
                                <h4 class="text-warning">${summary.avgOrderValue.toFixed(2)}</h4>
                                <p class="mb-0">Avg Order Value</p>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        } else {
            popup.error({message: json.message || 'Error loading summary report.'});
            return `<div class="alert alert-danger">Error loading summary report: ${json.message || 'Unknown error'}</div>`;
        }
    } catch (err) {
        popup.error({message: 'Network error loading summary report.'});
        return '<div class="alert alert-danger">Network error loading summary report.</div>';
    }
}

async function loadSalesReport() {
    const popup = new Notification();
    try {
        const response = await fetch('ReportServlet?action=sales');
        const json = await response.json();
        if (json.status) {
            let html = `
                <div class="report-section">
                    <h5><i class="bi bi-graph-up me-2"></i>Sales Performance</h5>
                    <div class="row mb-4">
                        <div class="col-md-6">
                            <h6>Monthly Sales Trends</h6>
                            <div class="table-responsive">
                                <table class="table table-striped">
                                    <thead>
                                        <tr>
                                            <th>Period</th>
                                            <th>Orders</th>
                                            <th>Revenue</th>
                                        </tr>
                                    </thead>
                                    <tbody>
            `;

            if (json.monthlySales && json.monthlySales.length > 0) {
                const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                json.monthlySales.forEach(item => {
                    html += `
                        <tr>
                            <td>${monthNames[item.month - 1]} ${item.year}</td>
                            <td>${item.orderCount}</td>
                            <td>${item.revenue.toFixed(2)}</td>
                        </tr>
                    `;
                });
            } else {
                html += '<tr><td colspan="3" class="text-center">No sales data available</td></tr>';
            }

            html += `
                                    </tbody>
                                </table>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <h6>Top Selling Products</h6>
                            <div class="table-responsive">
                                <table class="table table-striped">
                                    <thead>
                                        <tr>
                                            <th>Product</th>
                                            <th>Units Sold</th>
                                            <th>Revenue</th>
                                        </tr>
                                    </thead>
                                    <tbody>
            `;

            if (json.topProducts && json.topProducts.length > 0) {
                json.topProducts.forEach(item => {
                    html += `
                        <tr>
                            <td>${item.title}</td>
                            <td>${item.totalSold}</td>
                            <td>${item.revenue.toFixed(2)}</td>
                        </tr>
                    `;
                });
            } else {
                html += '<tr><td colspan="3" class="text-center">No product data available</td></tr>';
            }

            html += `
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            return html;
        } else {
            popup.error({message: json.message || 'Error loading sales report.'});
            return `<div class="alert alert-danger">Error loading sales report: ${json.message || 'Unknown error'}</div>`;
        }
    } catch (err) {
        popup.error({message: 'Network error loading sales report.'});
        return '<div class="alert alert-danger">Network error loading sales report.</div>';
    }
}

async function loadInventoryReport() {
    const popup = new Notification();
    try {
        const response = await fetch('ReportServlet?action=inventory');
        const json = await response.json();
        if (json.status) {
            let html = `
                <div class="report-section">
                    <h5><i class="bi bi-boxes me-2"></i>Inventory Analysis</h5>
                    <div class="row mb-4">
                        <div class="col-md-6">
                            <h6>Low Stock Alert</h6>
                            <div class="table-responsive">
                                <table class="table table-striped">
                                    <thead>
                                        <tr>
                                            <th>Product</th>
                                            <th>Brand</th>
                                            <th>Stock</th>
                                            <th>Price</th>
                                        </tr>
                                    </thead>
                                    <tbody>
            `;

            if (json.lowStockProducts && json.lowStockProducts.length > 0) {
                json.lowStockProducts.forEach(item => {
                    const rowClass = item.qty === 0 ? 'table-danger' : 'table-warning';
                    html += `
                        <tr class="${rowClass}">
                            <td>${item.title}</td>
                            <td>${item.brandName}</td>
                            <td>${item.qty}</td>
                            <td>${item.price.toFixed(2)}</td>
                        </tr>
                    `;
                });
            } else {
                html += '<tr><td colspan="4" class="text-center text-success">All products are well stocked!</td></tr>';
            }

            html += `
                                    </tbody>
                                </table>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <h6>Stock Value by Brand</h6>
                            <div class="table-responsive">
                                <table class="table table-striped">
                                    <thead>
                                        <tr>
                                            <th>Brand</th>
                                            <th>Products</th>
                                            <th>Stock Value</th>
                                        </tr>
                                    </thead>
                                    <tbody>
            `;

            if (json.stockByBrand && json.stockByBrand.length > 0) {
                json.stockByBrand.forEach(item => {
                    html += `
                        <tr>
                            <td>${item.brandName}</td>
                            <td>${item.productCount}</td>
                            <td>${item.stockValue.toFixed(2)}</td>
                        </tr>
                    `;
                });
            } else {
                html += '<tr><td colspan="3" class="text-center">No brand data available</td></tr>';
            }

            html += `
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            return html;
        } else {
            popup.error({message: json.message || 'Error loading inventory report.'});
            return `<div class="alert alert-danger">Error loading inventory report: ${json.message || 'Unknown error'}</div>`;
        }
    } catch (err) {
        popup.error({message: 'Network error loading inventory report.'});
        return '<div class="alert alert-danger">Network error loading inventory report.</div>';
    }
}

async function loadCustomerReport() {
    const popup = new Notification();
    try {
        const response = await fetch('ReportServlet?action=customers');
        const json = await response.json();
        if (json.status) {
            let html = `
                <div class="report-section">
                    <h5><i class="bi bi-people me-2"></i>Customer Analytics</h5>
                    <div class="row mb-4">
                        <div class="col-md-6">
                            <h6>Top Customers</h6>
                            <div class="table-responsive">
                                <table class="table table-striped">
                                    <thead>
                                        <tr>
                                            <th>Customer</th>
                                            <th>Orders</th>
                                            <th>Total Spent</th>
                                        </tr>
                                    </thead>
                                    <tbody>
            `;

            if (json.topCustomers && json.topCustomers.length > 0) {
                json.topCustomers.forEach(customer => {
                    html += `
                        <tr>
                            <td>${customer.firstName} ${customer.lastName}</td>
                            <td>${customer.orderCount}</td>
                            <td>${customer.totalSpent.toFixed(2)}</td>
                        </tr>
                    `;
                });
            } else {
                html += '<tr><td colspan="3" class="text-center">No customer data available</td></tr>';
            }

            html += `
                                    </tbody>
                                </table>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <h6>Registration Trends</h6>
                            <div class="table-responsive">
                                <table class="table table-striped">
                                    <thead>
                                        <tr>
                                            <th>Period</th>
                                            <th>New Customers</th>
                                        </tr>
                                    </thead>
                                    <tbody>
            `;

            if (json.registrationTrends && json.registrationTrends.length > 0) {
                const monthNames = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
                json.registrationTrends.forEach(item => {
                    html += `
                        <tr>
                            <td>${monthNames[item.month - 1]} ${item.year}</td>
                            <td>${item.newCustomers}</td>
                        </tr>
                    `;
                });
            } else {
                html += '<tr><td colspan="2" class="text-center">No registration data available</td></tr>';
            }

            html += `
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            return html;
        } else {
            popup.error({message: json.message || 'Error loading customer report.'});
            return `<div class="alert alert-danger">Error loading customer report: ${json.message || 'Unknown error'}</div>`;
        }
    } catch (err) {
        popup.error({message: 'Network error loading customer report.'});
        return '<div class="alert alert-danger">Network error loading customer report.</div>';
    }
}

async function loadBrandReport() {
    const popup = new Notification();
    try {
        const response = await fetch('ReportServlet?action=brands');
        const json = await response.json();
        if (json.status) {
            let html = `
                <div class="report-section">
                    <h5><i class="bi bi-tags me-2"></i>Brand Performance</h5>
                    <div class="table-responsive">
                        <table class="table table-striped">
                            <thead>
                                <tr>
                                    <th>Brand</th>
                                    <th>Products</th>
                                    <th>Avg Price</th>
                                    <th>Total Stock</th>
                                    <th>Units Sold</th>
                                    <th>Revenue</th>
                                </tr>
                            </thead>
                            <tbody>
            `;

            if (json.brandPerformance && json.brandPerformance.length > 0) {
                json.brandPerformance.forEach(brand => {
                    html += `
                        <tr>
                            <td><strong>${brand.brandName}</strong></td>
                            <td>${brand.productCount}</td>
                            <td>${brand.avgPrice.toFixed(2)}</td>
                            <td>${brand.totalStock}</td>
                            <td>${brand.totalSold}</td>
                            <td>${brand.revenue.toFixed(2)}</td>
                        </tr>
                    `;
                });
            } else {
                html += '<tr><td colspan="6" class="text-center">No brand data available</td></tr>';
            }

            html += `
                            </tbody>
                        </table>
                    </div>
                </div>
            `;

            return html;
        } else {
            popup.error({message: json.message || 'Error loading brand report.'});
            return `<div class="alert alert-danger">Error loading brand report: ${json.message || 'Unknown error'}</div>`;
        }
    } catch (err) {
        popup.error({message: 'Network error loading brand report.'});
        return '<div class="alert alert-danger">Network error loading brand report.</div>';
    }
}

// SETTINGS
async function loadSettings() {
    const popup = new Notification();
    const nameInput = document.getElementById('adminName');
    const emailInput = document.getElementById('adminEmail');

    if (!nameInput || !emailInput)
        return;

    try {
        const response = await fetch('AdminSettingsServlet?action=profile');
        const json = await response.json();
        if (json.status) {
            nameInput.value = json.admin.name || '';
            emailInput.value = json.admin.email || '';
        } else {
            popup.error({message: json.message || 'Failed to load profile.'});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

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
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });
        const json = await response.json();
        if (json.status) {
            popup.success({message: 'Profile updated successfully!'});
            form.adminPassword.value = '';
        } else {
            popup.error({message: json.message || 'Failed to update profile.'});
        }
    } catch (err) {
        popup.error({message: 'Error: ' + err.message});
    }
}

// UTILITY FUNCTIONS
function showLoading(containerId) {
    const container = document.getElementById(containerId);
    if (container) {
        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'loading-overlay';
        loadingDiv.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>';
        loadingDiv.style.cssText = 'position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(255,255,255,0.8); display: flex; justify-content: center; align-items: center; z-index: 1000;';
        container.style.position = 'relative';
        container.appendChild(loadingDiv);
    }
}

function hideLoading(containerId) {
    const container = document.getElementById(containerId);
    if (container) {
        const loadingOverlay = container.querySelector('.loading-overlay');
        if (loadingOverlay) {
            loadingOverlay.remove();
        }
    }
}

function switchTab(tabName) {
    const tabLink = document.querySelector(`#${tabName}-tab`);
    if (tabLink) {
        const tab = new bootstrap.Tab(tabLink);
        tab.show();
    }
}

// Search and filter functions
function searchTable(tableId, searchInputId) {
    const input = document.getElementById(searchInputId);
    const table = document.getElementById(tableId);

    if (!input || !table)
        return;

    const filter = input.value.toLowerCase();
    const rows = table.getElementsByTagName('tbody')[0].getElementsByTagName('tr');

    for (let i = 0; i < rows.length; i++) {
        const cells = rows[i].getElementsByTagName('td');
        let found = false;

        for (let j = 0; j < cells.length; j++) {
            if (cells[j].textContent.toLowerCase().includes(filter)) {
                found = true;
                break;
            }
        }

        rows[i].style.display = found ? '' : 'none';
    }
}

// Refresh functions
function refreshDashboard() {
    loadDashboard();
}

function refreshProducts() {
    loadProducts();
}

function refreshOrders() {
    loadOrders();
}

function refreshCustomers() {
    loadCustomers();
}

function refreshReports() {
    loadReports();
}

function refreshEntities() {
    loadEntityManagement();
}