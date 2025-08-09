async function loadCartItems() {
    const popup = new Notification();
    try {
        const response = await fetch("LoadCartItems");
        if (!response.ok) {
            popup.error({
                message: 'Cart items loading failed...'
            });
            return;
        }
        
        const json = await response.json();
        if (json.status) {
            const cart_item_container = document.getElementById("cart-item-container");
            if (!cart_item_container) {
                console.log("Cart container not found, might be on a different page");
                return;
            }
            
            cart_item_container.innerHTML = "";
            let total = 0;
            let totalQty = 0;
            
            json.cartItems.forEach(cart => {
                let productSubTotal = cart.product.price * cart.qty;
                total += productSubTotal;
                totalQty += cart.qty;
                let tableData = `<tr id="cart-item-row-${cart.product.id}">
                                        <td class="product-remove">
                                            <a href="#" onclick="removeFromCart(${cart.product.id}); return false;" class="remove-wishlist">
                                                <i class="fa fa-times"></i>
                                            </a>
                                        </td>
                                        <td class="product-thumbnail col-1">
                                            <a href="product-details.html?id=${cart.product.id}">
                                                <img class="col-4" src="product-images/${cart.product.id}/image1.png" alt="Product" style="min-width: 100px;">
                                            </a>
                                        </td>
                                        <td class="product-title">
                                            <a href="product-details.html?id=${cart.product.id}">${cart.product.title}</a>
                                        </td>
                                        <td class="product-price" data-title="Price">
                                            <span class="currency-symbol">Rs. </span>
                                            <span>${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(cart.product.price)}</span>
                                        </td>
                                        <td class="product-quantity" data-title="Qty">
                                            <div class="pro-qty">
                                                <input type="number" class="quantity-input" value="${cart.qty}" min="1"
                                                       onchange="updateCartQuantity(${cart.product.id}, this.value)">
                                            </div>
                                        </td>
                                        <td class="product-subtotal" data-title="Subtotal">
                                            <span class="currency-symbol">Rs. </span>
                                            <span>${new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(productSubTotal)}</span>
                                        </td>
                                    </tr>`;
                cart_item_container.innerHTML += tableData;
            });

            // Update totals if the elements exist
            const totalQtyElement = document.getElementById("order-total-quantity");
            const totalAmountElement = document.getElementById("order-total-amount");
            
            if (totalQtyElement) {
                totalQtyElement.innerHTML = totalQty;
            }
            if (totalAmountElement) {
                totalAmountElement.innerHTML = new Intl.NumberFormat("en-US", {minimumFractionDigits: 2}).format(total);
            }
        } else {
            popup.error({
                message: json.message || 'Failed to load cart items'
            });
        }
    } catch (error) {
        console.error('Error loading cart:', error);
        popup.error({
            message: 'Failed to load cart items'
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
        
        // Show loading state (optional)
        const cartItemRow = document.getElementById(`cart-item-row-${productId}`);
        if (cartItemRow) {
            cartItemRow.style.opacity = '0.5';
        }

        const response = await fetch(`RemoveFromCart?productId=${productId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
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
        if (!json.status) {
            // Restore opacity if failed
            if (cartItemRow) {
                cartItemRow.style.opacity = '1';
            }
            throw new Error(json.message || 'Failed to remove from cart');
        }
        
        // Successfully removed
        popup.success({
            message: json.message || 'Product removed from cart successfully'
        });
        
        // Remove the row from the table
        if (cartItemRow) {
            cartItemRow.remove();
        }
        
        // Update mini cart and reload cart items to refresh totals
        if (typeof updateMiniCart === 'function') {
            await updateMiniCart();
        }
        
        // Reload cart items to update totals
        await loadCartItems();
        
    } catch (error) {
        console.error('Error removing from cart:', error);
        popup.error({
            message: 'Failed to remove item from cart. Please try again.'
        });
        
        // Restore opacity on error
        const cartItemRow = document.getElementById(`cart-item-row-${productId}`);
        if (cartItemRow) {
            cartItemRow.style.opacity = '1';
        }
    }
}

async function updateCartQuantity(productId, newQuantity) {
    const popup = new Notification({
        position: 'top-right',
        duration: 3000
    });
    
    if (parseInt(newQuantity) <= 0) {
        popup.error({
            message: 'Quantity must be greater than 0'
        });
        // Reload cart to restore original quantity
        await loadCartItems();
        return;
    }
    
    try {
        const response = await fetch(`UpdateCartQuantity?productId=${productId}&quantity=${newQuantity}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        
        const json = await response.json();
        if (!json.status) {
            throw new Error(json.message || 'Failed to update quantity');
        }
        
        popup.success({
            message: json.message || 'Quantity updated successfully'
        });
        
        // Update mini cart and reload cart items to refresh totals
        if (typeof updateMiniCart === 'function') {
            await updateMiniCart();
        }
        
        // Reload cart items to update totals
        await loadCartItems();
        
    } catch (error) {
        console.error('Error updating cart quantity:', error);
        popup.error({
            message: 'Failed to update quantity. Please try again.'
        });
        
        // Reload cart to restore original quantity
        await loadCartItems();
    }
}