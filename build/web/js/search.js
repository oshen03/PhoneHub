// Global state for filters
let filters = {
    brand: [],
    color: [],
    storage: [],
    quality: [],
    priceRange: { min: 0, max: 1000000 }
};

// Function to show loading state
function showLoading() {
    const gridContainer = document.getElementById('st-product-container');
    const listContainer = document.getElementById('st-product-container-list');
    
    const loadingHtml = `<div class="col-12 text-center">
        <p>Loading products...</p>
    </div>`;
    
    if (gridContainer) gridContainer.innerHTML = loadingHtml;
    if (listContainer) listContainer.innerHTML = loadingHtml;
}

// Initialize price range slider
function initPriceRange() {
    const slider = $("#slider-range");
    if (slider.length) {
        slider.slider({
            range: true,
            min: 0,
            max: 1000000,
            values: [0, 1000000],
            slide: function(event, ui) {
                $("#price-range-label").text("Rs. " + ui.values[0] + " - Rs. " + ui.values[1]);
                filters.priceRange = { min: ui.values[0], max: ui.values[1] };
                searchProduct(0);
            }
        });
        $("#price-range-label").text("Rs. " + slider.slider("values", 0) + " - Rs. " + slider.slider("values", 1));
    }
}

async function loadData() {
    const popup = new Notification();
    try {
        showLoading();
        const response = await fetch("LoadData");
        if (!response.ok) throw new Error('Failed to load data');
        
        const json = await response.json();
        console.log("LoadData response:", json); // Debug log
        
        // Load filter options
        if (json.brands) loadOptions("brand", json.brands, "name");
        if (json.qualities) loadOptions("quality", json.qualities, "value");
        if (json.colors) loadOptions("color", json.colors, "value");
        if (json.storages) loadOptions("storage", json.storages, "value");
        
        // Initialize price range if not already done
        initPriceRange();
        
        // Perform initial search
        await searchProduct(0);
        
    } catch (error) {
        console.error("Error in loadData:", error);
        notif.error({ message: 'Failed to load data: ' + error.message });
    }
}

function loadOptions(prefix, dataList, property) {
    let filterList = document.getElementById(`${prefix}-filter-list`);
    if (!filterList) return;

    filterList.innerHTML = ""; // Clear existing options
    
    dataList.forEach(item => {
        const li = document.createElement('li');
        li.innerHTML = `
            <input type="checkbox" class="filter-checkbox" 
                   id="${prefix}-${item.id}" 
                   data-type="${prefix}" 
                   data-id="${item.id}"
                   value="${item[property]}">
            <label for="${prefix}-${item.id}">${item[property]}</label>
        `;
        filterList.appendChild(li);
    });

    // Add event listeners to checkboxes
    const checkboxes = filterList.querySelectorAll('input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const type = this.dataset.type;
            const id = this.dataset.id;
            
            if (this.checked) {
                if (!filters[type].includes(id)) {
                    filters[type].push(id);
                }
            } else {
                filters[type] = filters[type].filter(item => item !== id);
            }
            
            searchProduct(0); // Refresh search results
        });
    });
}

async function searchProduct(firstResult) {
    const popup = new Notification();
    try {
        showLoading();
        const searchKey = document.getElementById('search-key')?.value || '';
        const sortBy = document.getElementById('st-sort')?.value || '';
        
        // Build URL parameters
        const params = new URLSearchParams();
        params.append('searchKey', searchKey);
        params.append('firstResult', firstResult);
        params.append('sortBy', sortBy);
        
        // Add filter parameters
        if (filters.brand.length > 0) {
            filters.brand.forEach(id => params.append('brand[]', id));
        }
        if (filters.quality.length > 0) {
            filters.quality.forEach(id => params.append('quality[]', id));
        }
        if (filters.color.length > 0) {
            filters.color.forEach(id => params.append('color[]', id));
        }
        if (filters.storage.length > 0) {
            filters.storage.forEach(id => params.append('storage[]', id));
        }
        if (filters.priceRange) {
            params.append('priceRange[min]', filters.priceRange.min);
            params.append('priceRange[max]', filters.priceRange.max);
        }

        console.log("Searching with params:", params.toString()); // Debug log

        const response = await fetch(`SearchProducts?${params.toString()}`);
        if (!response.ok) throw new Error('Failed to search products');
        
        const json = await response.json();
        console.log("Search response:", json); // Debug log
        
        updateProductView(json);
        notif.success({ message: 'Products loaded successfully' });
    } catch (error) {
        console.error("Error in searchProduct:", error);
        notif.error({ message: 'Error searching products: ' + error.message });
    }
}

function updatePagination(totalProducts, currentFirst) {
    const itemsPerPage = 9; // Same as your backend
    const totalPages = Math.ceil(totalProducts / itemsPerPage);
    const currentPage = Math.floor(currentFirst / itemsPerPage) + 1;

    const paginationInfo = document.getElementById('pagination-info');
    if (paginationInfo) {
        paginationInfo.textContent = `Showing ${currentFirst + 1}-${Math.min(currentFirst + itemsPerPage, totalProducts)} of ${totalProducts} item(s)`;
    }

    const paginationLinks = document.getElementById('pagination-links');
    if (!paginationLinks) return;
    
    let linksHtml = '';

    // Previous button
    if (currentPage > 1) {
        linksHtml += `<li>
            <a class="Previous" href="#" onclick="searchProduct(${(currentPage - 2) * itemsPerPage}); return false;">Previous</a>
        </li>`;
    }

    // Page numbers
    for (let i = 1; i <= totalPages; i++) {
        linksHtml += `<li class="${i === currentPage ? 'active' : ''}">
            <a href="#" onclick="searchProduct(${(i - 1) * itemsPerPage}); return false;">${i}</a>
        </li>`;
    }

    // Next button
    if (currentPage < totalPages) {
        linksHtml += `<li>
            <a class="Next" href="#" onclick="searchProduct(${currentPage * itemsPerPage}); return false;">Next</a>
        </li>`;
    }

    paginationLinks.innerHTML = linksHtml;
}

function clearAllFilters() {
    filters = {
        brand: [],
        color: [],
        storage: [],
        quality: [],
        priceRange: { min: 0, max: 1000000 }
    };

    // Uncheck all checkboxes
    document.querySelectorAll('.filter-checkbox').forEach(checkbox => {
        checkbox.checked = false;
    });

    // Reset price range slider
    const slider = $("#slider-range");
    if (slider.length) {
        slider.slider("values", [0, 1000000]);
        $("#price-range-label").text("Rs. " + slider.slider("values", 0) + " - Rs. " + slider.slider("values", 1));
    }

    // Refresh search results
    searchProduct(0);
}

function updateProductView(json) {
    // Update product container for grid view
    const gridContainer = document.getElementById('st-product-container');
    let gridHtml = '';
    
    if (json.products && json.products.length > 0) {
        json.products.forEach(product => {
            gridHtml += `
                <div class="col-lg-4 col-md-4 col-sm-6">
                    <div class="single-product-wrap">
                    <div class="product-image">
                        <a href="single-product.html?id=${product.id}">
                            <img src="product-images/${product.id}/image1.png" alt="${product.title}">
                        </a>
                        <span class="sticker">New</span>
                    </div>
                        <div class="product_desc">
                            <div class="product_desc_info">
                                <div class="product-review">
                                    <h5 class="manufacturer">
                                       <a href="#">${product.model != null && product.model.brand != null ? product.model.brand.name : 'Unknown Brand'}</a>
                                    </h5>
                                </div>
                                <h4><a class="product_name" href="single-product.html?id=${product.id}">${product.title}</a></h4>
                                <div class="price-box">
                                    <span class="new-price">Rs. ${product.price ? product.price.toLocaleString() : 'N/A'}</span>
                                </div>
                            </div>
                            <div class="add-actions">
                                <ul class="add-actions-link">
                                    <li class="add-cart active"><a href="#" onclick="addToCart(${product.id}, 1); return false;">Add to cart</a></li>
                                    <li><a class="links-details" href="#" onclick="addToWishlist(${product.id}); return false;"><i class="fa fa-heart-o"></i></a></li>
                                    <li><a href="#" title="quick view" class="quick-view-btn" data-toggle="modal" data-target="#exampleModalCenter"><i class="fa fa-eye"></i></a></li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
    } else {
        gridHtml = '<div class="col-12 text-center"><p>No products found</p></div>';
    }
    
    if (gridContainer) {
        gridContainer.innerHTML = gridHtml;
    }

    // Update product container for list view
    const listContainer = document.getElementById('st-product-container-list');
    let listHtml = '';
    
    if (json.products && json.products.length > 0) {
        json.products.forEach(product => {
            listHtml += `
                <div class="row product-layout-list">
                    <div class="col-lg-3 col-md-5">
                        <div class="product-image">
                            <a href="single-product.html?id=${product.id}">
                                <img src="product-images/${product.id}/image1.png" alt="${product.title}">
                            </a>
                            <span class="sticker">New</span>
                        </div>
                    </div>
                    <div class="col-lg-5 col-md-7">
                        <div class="product_desc">
                            <div class="product_desc_info">
                                <div class="product-review">
                                    <h5 class="manufacturer">
                                        <a href="#">${product.model != null && product.model.brand != null ? product.model.brand.name : 'Unknown Brand'}</a>
                                    </h5>
                                </div>
                                <h4><a class="product_name" href="single-product.html?id=${product.id}">${product.title}</a></h4>
                                <div class="price-box">
                                    <span class="new-price">Rs. ${product.price ? product.price.toLocaleString() : 'N/A'}</span>
                                </div>
                                <p>${product.description || ''}</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-lg-4">
                        <div class="shop-add-action">
                            <ul class="add-actions-link">
                                <li class="add-cart"><a href="#" onclick="addToCart(${product.id}, 1); return false;">Add to cart</a></li>
                                <li><a class="links-details" href="#" onclick="addToWishlist(${product.id}); return false;"><i class="fa fa-heart-o"></i></a></li>
                                <li><a href="#" title="quick view" class="quick-view-btn" data-toggle="modal" data-target="#exampleModalCenter"><i class="fa fa-eye"></i></a></li>
                            </ul>
                        </div>
                    </div>
                </div>
            `;
        });
    } else {
        listHtml = '<div class="col-12 text-center"><p>No products found</p></div>';
    }
    
    if (listContainer) {
        listContainer.innerHTML = listHtml;
    }

    // Update pagination
    updatePagination(json.totalProducts || 0, json.firstResult || 0);
    
    // Update toolbar amount
    const toolbarAmount = document.querySelector('.toolbar-amount span');
    if (toolbarAmount && json.totalProducts !== undefined) {
        const start = (json.firstResult || 0) + 1;
        const end = Math.min((json.firstResult || 0) + 9, json.totalProducts);
        toolbarAmount.textContent = `Showing ${start} to ${end} of ${json.totalProducts}`;
    }
}

async function addToCart(productId, qty) {
    try {
        const response = await fetch('AddToCart', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `productId=${productId}&qty=${qty}`
        });
        
        if (response.ok) {
            notif.success({ message: 'Product added to cart successfully!' });
        } else {
            notif.error({ message: 'Failed to add product to cart' });
        }
    } catch (error) {
        notif.error({ message: 'Error adding to cart: ' + error.message });
    }
}

async function addToWishlist(productId) {
    try {
        const response = await fetch('AddToWishlist', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `productId=${productId}`
        });
        
        if (response.ok) {
            notif.success({ message: 'Product added to wishlist successfully!' });
        } else {
            notif.error({ message: 'Failed to add product to wishlist' });
        }
    } catch (error) {
        notif.error({ message: 'Error adding to wishlist: ' + error.message });
    }
}

// Add this function for backward compatibility
function indexOnloadFunctions() {
    // This function is called from the HTML body onload
    // The main initialization is handled by DOMContentLoaded below
    console.log("indexOnloadFunctions called");
}

// Initialize everything on page load
document.addEventListener('DOMContentLoaded', function() {
    console.log("DOM Content Loaded - Initializing search functionality");
    
    // Load initial data and filters
    loadData();
    
    // Handle sort change
    const sortSelect = document.getElementById('st-sort');
    if (sortSelect) {
        sortSelect.addEventListener('change', () => {
            console.log("Sort changed:", sortSelect.value);
            searchProduct(0);
        });
    }
    
    // Handle search form submission
    const searchForm = document.querySelector('form.hm-searchbox');
    if (searchForm) {
        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            console.log("Search form submitted");
            searchProduct(0);
        });
    }

    // Add debug info
    console.log("Search form initialized:", !!searchForm);
    console.log("Sort select initialized:", !!sortSelect);
});