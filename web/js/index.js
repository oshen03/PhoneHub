async function indexOnloadFunctions() {
    try {
// First check login status and update sign in/out
        await updateSignInOut();
        // Then handle cart and product data
        await checkSessionCart();
        await updateMiniCart();
        await loadProductData();
    } catch (error) {
        console.error('Error in page initialization:', error);
        const notif = Notification({
            position: 'top-right',
            duration: 3000
        });
        notif.error({message: 'Error loading page. Please refresh.'});
    }
}
// Dynamically update Sign In/Sign Out in settings list
async function updateSignInOut() {
    try {
        const response = await fetch('CheckSessionCart', {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Cache-Control': 'no-cache'
            }
        });

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }

        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new TypeError('Expected JSON response but got ' + contentType);
        }

        const json = await response.json();
        console.log('Login check response:', json);

        // Update Sign In/Out Link
        const signInOutLi = document.getElementById('sign-in-out');
        if (!signInOutLi) {
            throw new Error('Sign In/Out element not found');
        }

        // Update My Account Link
        const myAccountLi = document.getElementById('my-account-link');
        if (!myAccountLi) {
            throw new Error('My Account element not found');
        }

        if (json.status === true) {
            // Update Sign Out link
            signInOutLi.innerHTML = '<a href="#" class="sign-out-link">Sign Out</a>';
            const signOutLink = signInOutLi.querySelector('.sign-out-link');
            
            // Update My Account link when logged in
            myAccountLi.innerHTML = '<a href="my-account.html">My Account</a>';
            
            signOutLink.addEventListener('click', async function(e) {
                e.preventDefault();
                try {
                    const signOutResponse = await fetch('SignOut');
                    if (!signOutResponse.ok) {
                        throw new Error('Sign out request failed');
                    }
                    
                    const result = await signOutResponse.json();
                    if (result.status) {
                        window.location.href = 'login-register.html';
                    } else {
                        throw new Error('Server returned failure status');
                    }
                } catch (error) {
                    console.error('Sign out error:', error);
                    const notif = Notification({
                        position: 'top-right',
                        duration: 3000
                    });
                    notif.error({ message: 'Error signing out: ' + error.message });
                }
            });
        } else {
            // Update Sign In link when logged out
            signInOutLi.innerHTML = '<a href="login-register.html">Sign In</a>';
            // Update My Account link to redirect to login when logged out
            myAccountLi.innerHTML = '<a href="login-register.html">My Account</a>';
        }
    } catch (err) {
        console.error('Error in updateSignInOut:', err);
        const notif = Notification({
            position: 'top-right',
            duration: 3000
        });
        notif.error({ message: 'Error: ' + err.message });
    }
}

async function checkSessionCart() {
    const popup = new Notification();
    const response = await fetch("CheckSessionCart");
    if (!response.ok) {
        popup.error({
            message: "Something went wrong! Try again shortly"
        });
    }
}

async function loadProductData() {
    const popup = new Notification();
    const response = await fetch("LoadHomeData");
    if (response.ok) {
        const json = await response.json();
        if (json.status) {
            console.log(json);
            loadBrands(json);
            loadNewArrivals(json);
        } else {
            popup.error({
                message: "Something went wrong! Try again shortly"
            });
        }
    } else {
        popup.error({
            message: "Something went wrong! Try again shortly"
        });
    }
}

function loadBrands(json) {
    const product_brand_container = document.getElementById("product-brand-container");
    const product_brand_card = document.getElementById("product-brand-card");
    // Check if elements exist - if not, skip brand loading
    if (!product_brand_container || !product_brand_card) {
        console.log("Brand elements not found, skipping brand loading");
        return;
    }

    product_brand_container.innerHTML = "";
    let card_delay = 200;
    json.brandList.forEach(item => {
        let product_brand_card_clone = product_brand_card.cloneNode(true);
        product_brand_card_clone.querySelector("#product-brand-mini-card")
                .setAttribute("data-sal", "zoom-out");
        product_brand_card_clone.querySelector("#product-brand-mini-card")
                .setAttribute("data-sal-delay", String(card_delay));
        product_brand_card_clone.querySelector("#product-brand-a")
                .href = "search.html";
        product_brand_card_clone.querySelector("#product-brand-title")
                .innerHTML = item.name;
        product_brand_container.appendChild(product_brand_card_clone);
        card_delay += 100;
        if (typeof sal !== 'undefined') {
            sal();
        }
    });
}

function loadNewArrivals(json) {
    const carousel_container = document.querySelector("#new-arrival-product-container .product-active");
    carousel_container.innerHTML = "";
    json.productList.forEach(item => {
        // Get brand name with proper null checks
        const brandName = getBrandName(item);
        let product_card = `<div class="col-lg-12">
                                <div class="single-product-wrap">
                                    <div class="product-image">
                                        <a href="single-product.html?id=${item.id}">
                                            <img  src="product-images/${item.id}/image1.png" alt="Li's Product Image">
                                        </a>
                                        <span class="sticker">New</span>
                                    </div>
                                    <div class="product_desc">
                                        <div class="product_desc_info">
                                            <div class="product-review">
                                                <h5 class="manufacturer">
                                                    <a href="shop-left-sidebar.html">${brandName}</a>
                                                </h5>
                                                <div class="rating-box">
                                                    <ul class="rating">
                                                        <li><i class="fa fa-star-o"></i></li>
                                                        <li><i class="fa fa-star-o"></i></li>
                                                        <li><i class="fa fa-star-o"></i></li>
                                                        <li class="no-star"><i class="fa fa-star-o"></i></li>
                                                        <li class="no-star"><i class="fa fa-star-o"></i></li>
                                                    </ul>
                                                </div>
                                            </div>
                                            <h4><a class="product_name" href="single-product.html?id=${item.id}">${item.title}</a></h4>
                                            <div class="price-box">
                                                <span class="new-price new-price-2">Lkr ${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(item.price)}</span>
                                            </div>
                                        </div>
                                        <div class="add-actions">
                                            <ul class="add-actions-link">
                                                <li class="add-cart active"><a href="#" onclick="addToCart(${item.id}, 1)">Add to cart</a></li>
                                                <li><a href="#" title="quick view" class="quick-view-btn" data-toggle="modal" data-target="#exampleModalCenter"><i class="fa fa-eye"></i></a></li>
                                            </ul>
                                        </div>
                                    </div>
                                </div>
                            </div>`;
        carousel_container.innerHTML += product_card;
    });
    // Reinitialize the owl carousel after adding products
    $('.product-active').trigger('destroy.owl.carousel');
    $('.product-active').owlCarousel({
        loop: true,
        margin: 30,
        nav: true,
        dots: false,
        autoplay: true,
        autoplayTimeout: 5000,
        responsive: {
            0: {
                items: 1
            },
            600: {
                items: 2
            },
            1000: {
                items: 4
            }
        }
    });
}

// Helper function to safely get brand name from product object
function getBrandName(item) {
// Check if the JSON already includes brand name directly
    if (item.brand) {
        return item.brand;
    }

// Check through model relationship: product -> model -> brand
    if (item.model && item.model.brand && item.model.brand.name) {
        return item.model.brand.name;
    }

// Fallback to 'Unknown Brand'
    return 'Unknown Brand';
}

async function addToCart(productId, qty) {
    try {
        const response = await fetch("AddToCart?prId=" + productId + "&qty=" + qty);
        if (!response.ok)
            throw new Error('Network response was not ok');
        const json = await response.json();
        if (json.status) {
            const notif = Notification({
                position: 'top-right',
                duration: 3000
            });
            notif.success({
                message: json.message || "Product added to cart successfully"
            });
            // Update mini cart if the function exists
            if (typeof updateMiniCart === 'function') {
                updateMiniCart();
            }
        } else {
            const notif = Notification({
                position: 'top-right',
                duration: 3000
            });
            notif.error({
                message: json.message || "Something went wrong. Try again"
            });
        }
    } catch (error) {
        console.error('Error adding to cart:', error);
        const notif = Notification({
            position: 'top-right',
            duration: 3000
        });
        notif.error({
            message: "Something went wrong. Try again"
        });
    }
}