async function updateMiniCart() {
    try {
        const response = await fetch("LoadCartItems");
        if (!response.ok) {
            const notif = Notification({
                position: 'top-right',
                duration: 3000
            });
            notif.error({
                message: 'Failed to load cart items'
            });
            return;
        }
        
        const json = await response.json();
        if (!json.status) {
            const notif = Notification({
                position: 'top-right',
                duration: 3000
            });
            notif.error({
                message: json.message || 'Failed to load cart items'
            });
            return;
        }
        
        // Update cart count and totals
        let total = 0;
        let totalQty = 0;
        
        // Get mini cart elements
        const miniCartItems = document.getElementById('mini-cart-items');
        const miniCartCount = document.getElementById('mini-cart-count');
        const miniCartTotal = document.getElementById('mini-cart-total');
        const miniCartSubtotal = document.getElementById('mini-cart-subtotal');
        
        // Clear existing items
        miniCartItems.innerHTML = '';
        
        // Add new items
        json.cartItems.forEach(cart => {
            const productSubTotal = cart.product.price * cart.qty;
            total += productSubTotal;
            totalQty += cart.qty;
            
            const itemHtml = `
                <li id="mini-cart-item-${cart.product.id}">
                    <a href="product-details.html?id=${cart.product.id}" class="minicart-product-image">
                        <img src="product-images/${cart.product.id}/image1.png" alt="${cart.product.title}">
                    </a>
                    <div class="minicart-product-details">
                        <h6><a href="product-details.html?id=${cart.product.id}">${cart.product.title}</a></h6>
                        <span>Lkr ${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(cart.product.price)} x ${cart.qty}</span>
                    </div>
                    <button class="close" onclick="removeFromCart(${cart.product.id})">
                        <i class="fa fa-close"></i>
                    </button>
                </li>
            `;
            miniCartItems.innerHTML += itemHtml;
        });
        
        // Update totals
        miniCartCount.textContent = totalQty;
        miniCartTotal.textContent = new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(total);
        miniCartSubtotal.textContent = 'Lkr ' + new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(total);
        
    } catch (error) {
        console.error('Error updating mini cart:', error);
        const notif = Notification({
            position: 'top-right',
            duration: 3000
        });
        notif.error({
            message: error.message || "Failed to update mini cart"
        });
    }
}

// Override the existing addToCart function to update mini cart
async function addToCart(productId, qty) {
    const notif = Notification({
        position: 'top-right',
        duration: 3000
    });
    try {
        const response = await fetch(`AddToCart?prId=${productId}&qty=${qty}`);
        if (!response.ok) throw new Error('Failed to add to cart');
        
        const json = await response.json();
        if (!json.status) throw new Error(json.message || 'Failed to add to cart');
        
        notif.success({ message: json.message || 'Product added to cart successfully' });
        
        // Update mini cart after successful addition
        await updateMiniCart();
        
    } catch (error) {
        console.error('Error adding to cart:', error);
        notif.error({
            message: error.message || "Failed to add to cart"
        });
    }
}

async function removeFromCart(productId) {
    const popup = new Notification({
        position: 'top-right',
        duration: 3000
    });
    try {
            console.log('Removing item from cart:', productId);
        // First, remove the item visually for immediate feedback
        const miniCartItem = document.getElementById(`mini-cart-item-${productId}`);
        const cartItem = document.getElementById(`cart-item-row-${productId}`);
        if (miniCartItem) miniCartItem.style.display = 'none';
        if (cartItem) cartItem.style.display = 'none';

    const response = await fetch(`RemoveFromCart?productId=${productId}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    });

        if (!response.ok) throw new Error('Network response was not ok');
        
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
            throw new TypeError('Expected JSON response but got ' + contentType);
        }

        const json = await response.json();
        if (!json.status) {
            // If failed, show the items again
            if (miniCartItem) miniCartItem.style.display = '';
            if (cartItem) cartItem.style.display = '';
            throw new Error(json.message || 'Failed to remove from cart');
        }
        
        // Successfully removed, update the UI
        if (miniCartItem) miniCartItem.remove();
        if (cartItem) cartItem.remove();
        
        popup.success({
            message: json.message || 'Product removed from cart successfully'
        });
        
        // Update both mini cart and main cart
        await updateMiniCart();
        if (typeof loadCartItems === 'function') {
            await loadCartItems();
        }
        
    } catch (error) {
    console.error('Error removing from cart:', error);
    // Display an error message to the user
    const notif = Notification({
      position: 'top-right',
      duration: 3000
    });
    notif.error({
      message: 'Failed to remove item from cart. Please try again.'
    });
    }
}

// Initialize mini cart when page loads
window.addEventListener('load', () => {
    updateMiniCart();
});
