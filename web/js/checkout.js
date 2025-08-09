payhere.onCompleted = function onCompleted(orderId) {
    const popup = new Notification();
    popup.success({
        message: "Payment completed. OrderID:" + orderId
    });
};

// Payment window closed
payhere.onDismissed = function onDismissed() {
    // Note: Prompt user to pay again or show an error page
    console.log("Payment dismissed");
};

// Error occurred
payhere.onError = function onError(error) {
    // Note: show an error page
    console.log("Error:" + error);
};

async function loadCheckoutData() {
    const popup = new Notification();
    const response = await fetch("LoadCheckOutData");
    if (response.ok) { //200
        const json = await response.json();
        if (json.status) {
            console.log(json);
            const userAddress = json.userAddress;
            const cityList = json.cityList;
            const cartItems = json.cartList;
            const deliveryTypes = json.deliveryTypes;

            // load cities for billing
            let city_select = document.getElementById("city-select");
            cityList.forEach(city => {
                let option = document.createElement("option");
                option.value = city.id;
                option.innerHTML = city.name;
                city_select.appendChild(option);
            });

            // load cities for shipping
            let ship_city_select = document.getElementById("ship-city-select");
            cityList.forEach(city => {
                let option = document.createElement("option");
                option.value = city.id;
                option.innerHTML = city.name;
                ship_city_select.appendChild(option);
            });

            // load current address in billing details (disabled)
            let first_name = document.getElementById("first-name");
            let last_name = document.getElementById("last-name");
            let line_one = document.getElementById("line-one");
            let line_two = document.getElementById("line-two");
            let postal_code = document.getElementById("postal-code");
            let mobile = document.getElementById("mobile");
            let email = document.getElementById("email");

            first_name.value = userAddress.user.first_name;
            last_name.value = userAddress.user.last_name;
            city_select.value = userAddress.city.id;
            line_one.value = userAddress.lineOne;
            line_two.value = userAddress.lineTwo;
            postal_code.value = userAddress.postalCode;
            mobile.value = userAddress.mobile;
            email.value = userAddress.user.email;

            // Handle "Ship to different address" checkbox
            const ship_different_checkbox = document.getElementById("ship-box");
            const shipping_section = document.getElementById("ship-box-info");
            
            ship_different_checkbox.addEventListener("change", function() {
                if (ship_different_checkbox.checked) {
                    shipping_section.style.display = "block";
                } else {
                    shipping_section.style.display = "none";
                }
            });

            // cart-details
            let st_tbody = document.getElementById("st-tbody");
            let st_item_tr = document.getElementById("st-item-tr");
            let st_subtotal_tr = document.getElementById("st-subtotal-tr");
            let st_order_shipping_tr = document.getElementById("st-order-shipping-tr");
            let st_order_total_tr = document.getElementById("st-order-total-tr");

            st_tbody.innerHTML = "";

            let total = 0;
            let item_count = 0;
            cartItems.forEach(cart => {
                let st_item_tr_clone = st_item_tr.cloneNode(true);
                st_item_tr_clone.style.display = "table-row";
                st_item_tr_clone.querySelector("#st-product-title")
                        .innerHTML = cart.product.title;
                st_item_tr_clone.querySelector("#st-product-qty")
                        .innerHTML = cart.qty;
                item_count += cart.qty;
                let item_sub_total = Number(cart.qty) * Number(cart.product.price);

                st_item_tr_clone.querySelector("#st-product-price")
                        .innerHTML = new Intl.NumberFormat(
                                "en-US",
                                {minimumFractionDigits: 2})
                        .format(item_sub_total);
                st_tbody.appendChild(st_item_tr_clone);

                total += item_sub_total;
            });

            st_subtotal_tr.style.display = "table-row";
            st_subtotal_tr.querySelector("#st-product-total-amount")
                    .innerHTML = new Intl.NumberFormat(
                            "en-US",
                            {minimumFractionDigits: 2})
                    .format(total);
            st_tbody.appendChild(st_subtotal_tr);

            let shipping_charges = 0;
            
            // Function to calculate shipping
            function calculateShipping() {
                // Check if deliveryTypes array exists and has required elements
                if (!deliveryTypes || deliveryTypes.length < 2) {
                    console.log("Delivery types not available");
                    return;
                }
                
                let selectedCitySelect = ship_different_checkbox.checked ? ship_city_select : city_select;
                let cityName = selectedCitySelect.options[selectedCitySelect.selectedIndex].innerHTML;
                
                if (cityName === "Colombo") {
                    shipping_charges = item_count * deliveryTypes[0].price;
                } else {
                    shipping_charges = item_count * deliveryTypes[1].price;
                }

                st_order_shipping_tr.style.display = "table-row";
                st_order_shipping_tr.querySelector("#st-product-shipping-charges")
                        .innerHTML = new Intl.NumberFormat(
                                "en-US",
                                {minimumFractionDigits: 2})
                        .format(shipping_charges);
                
                if (!st_tbody.contains(st_order_shipping_tr)) {
                    st_tbody.appendChild(st_order_shipping_tr);
                }

                st_order_total_tr.style.display = "table-row";
                st_order_total_tr.querySelector("#st-order-total-amount")
                        .innerHTML = new Intl.NumberFormat(
                                "en-US",
                                {minimumFractionDigits: 2})
                        .format(shipping_charges + total);
                
                if (!st_tbody.contains(st_order_total_tr)) {
                    st_tbody.appendChild(st_order_total_tr);
                }
            }

            // Initial calculation
            calculateShipping();

            // Add event listeners for city changes
            city_select.addEventListener("change", calculateShipping);
            ship_city_select.addEventListener("change", calculateShipping);
            ship_different_checkbox.addEventListener("change", calculateShipping);

        } else {
            if (json.message === "empty-cart") {
                popup.error({
                    message: "Empty cart. Please add some product"
                });
                window.location = "index.html";
            } else {
                popup.error({
                    message: json.message
                });
            }
        }
    } else {
        if (response.status === 401) {
            window.location = "login-register.html";
        }
    }
}

async function checkout() {
    let ship_different_checkbox = document.getElementById("ship-box");
    let order_notes = document.getElementById("order-notes");
    
    let data = {};
    
    if (ship_different_checkbox.checked) {
        // Use shipping address
        let ship_first_name = document.getElementById("ship-first-name");
        let ship_last_name = document.getElementById("ship-last-name");
        let ship_city_select = document.getElementById("ship-city-select");
        let ship_line_one = document.getElementById("ship-line-one");
        let ship_line_two = document.getElementById("ship-line-two");
        let ship_postal_code = document.getElementById("ship-postal-code");
        let ship_mobile = document.getElementById("ship-mobile");
        
        data = {
            isCurrentAddress: false,
            firstName: ship_first_name.value,
            lastName: ship_last_name.value,
            citySelect: ship_city_select.value,
            lineOne: ship_line_one.value,
            lineTwo: ship_line_two.value,
            postalCode: ship_postal_code.value,
            mobile: ship_mobile.value,
            orderNotes: order_notes.value
        };
    } else {
        // Use current address (billing address)
        let first_name = document.getElementById("first-name");
        let last_name = document.getElementById("last-name");
        let city_select = document.getElementById("city-select");
        let line_one = document.getElementById("line-one");
        let line_two = document.getElementById("line-two");
        let postal_code = document.getElementById("postal-code");
        let mobile = document.getElementById("mobile");
        
        data = {
            isCurrentAddress: true,
            firstName: first_name.value,
            lastName: last_name.value,
            citySelect: city_select.value,
            lineOne: line_one.value,
            lineTwo: line_two.value,
            postalCode: postal_code.value,
            mobile: mobile.value,
            orderNotes: order_notes.value
        };
    }
    
    let dataJSON = JSON.stringify(data);

    const response = await fetch("CheckOut", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: dataJSON
    });

    const popup = new Notification();
    if (response.ok) {
        const json = await response.json();
        if (json.status) {
            console.log(json);
            // PayHere Process
            payhere.startPayment(json.payhereJson);
        } else {
            popup.error({
                message: json.message
            });
        }
    } else {
        popup.error({
            message: "Something went wrong. Please try again!"
        });
    }
}

// Load checkout data when page loads
window.addEventListener('load', loadCheckoutData);