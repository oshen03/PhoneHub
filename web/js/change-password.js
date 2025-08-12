let currentEmail = '';

async function sendVerificationCode() {
    const email = document.getElementById("adminEmail").value;
    const messageDiv = document.getElementById("messageDiv");
    
    // Clear previous messages
    messageDiv.classList.add("d-none");
    messageDiv.innerHTML = "";
    
    if (!email) {
        showMessage("Please enter your email address", "danger");
        return;
    }
    
    if (!isValidEmail(email)) {
        showMessage("Please enter a valid email address", "danger");
        return;
    }
    
    try {
        const response = await fetch("ChangePassword", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ 
                action: "sendCode",
                email: email 
            })
        });
        
        if (response.ok) {
            const result = await response.json();
            if (result.status) {
                currentEmail = email;
                showMessage("Verification code sent to your email!", "success");
                setTimeout(() => {
                    showStep2();
                }, 1500);
            } else {
                showMessage(result.message || "Failed to send verification code", "danger");
            }
        } else {
            showMessage("Error connecting to server", "danger");
        }
    } catch (error) {
        console.error("Send code error:", error);
        showMessage("Network error occurred", "danger");
    }
}

async function resetPassword() {
    const verificationCode = document.getElementById("verificationCode").value;
    const newPassword = document.getElementById("newPassword").value;
    const confirmPassword = document.getElementById("confirmPassword").value;
    const messageDiv = document.getElementById("messageDiv");
    
    // Clear previous messages
    messageDiv.classList.add("d-none");
    messageDiv.innerHTML = "";
    
    // Validation
    if (!verificationCode || !newPassword || !confirmPassword) {
        showMessage("Please fill all fields", "danger");
        return;
    }
    
    if (verificationCode.length !== 6 || !isNumeric(verificationCode)) {
        showMessage("Verification code must be 6 digits", "danger");
        return;
    }
    
    if (!isValidPassword(newPassword)) {
        showMessage("Password must be at least 8 characters with uppercase, lowercase, number and special character", "danger");
        return;
    }
    
    if (newPassword !== confirmPassword) {
        showMessage("Passwords do not match", "danger");
        return;
    }
    
    try {
        const response = await fetch("ChangePassword", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ 
                action: "resetPassword",
                email: currentEmail,
                verificationCode: verificationCode,
                newPassword: newPassword
            })
        });
        
        if (response.ok) {
            const result = await response.json();
            if (result.status) {
                showMessage("Password reset successful! Redirecting to login...", "success");
                setTimeout(() => {
                    window.location.href = "admin-login.html";
                }, 2000);
            } else {
                showMessage(result.message || "Failed to reset password", "danger");
            }
        } else {
            showMessage("Error connecting to server", "danger");
        }
    } catch (error) {
        console.error("Reset password error:", error);
        showMessage("Network error occurred", "danger");
    }
}

function showStep2() {
    document.getElementById("step1").classList.remove("active");
    document.getElementById("step2").classList.add("active");
    document.getElementById("step1-indicator").classList.remove("active");
    document.getElementById("step2-indicator").classList.add("active");
    document.querySelector(".reset-header p").textContent = "Enter verification code and new password";
}

function goBackToStep1() {
    document.getElementById("step2").classList.remove("active");
    document.getElementById("step1").classList.add("active");
    document.getElementById("step2-indicator").classList.remove("active");
    document.getElementById("step1-indicator").classList.add("active");
    document.querySelector(".reset-header p").textContent = "Enter your email to receive verification code";
    
    // Clear step 2 fields
    document.getElementById("verificationCode").value = "";
    document.getElementById("newPassword").value = "";
    document.getElementById("confirmPassword").value = "";
}

function showMessage(message, type) {
    const messageDiv = document.getElementById("messageDiv");
    messageDiv.className = `alert alert-${type}`;
    messageDiv.innerHTML = message;
    messageDiv.classList.remove("d-none");
}

function isValidEmail(email) {
    return /^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$/.test(email);
}

function isValidPassword(password) {
    return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=]).{8,}$/.test(password);
}

function isNumeric(str) {
    return /^\d+$/.test(str);
}

// Enable form submission on Enter key press
document.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        if (document.getElementById('step1').classList.contains('active')) {
            sendVerificationCode();
        } else if (document.getElementById('step2').classList.contains('active')) {
            resetPassword();
        }
    }
});