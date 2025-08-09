// Admin Session Check JavaScript
// Add this to admin.html to check session status on page load

function checkAdminSession() {
    fetch('AdminSessionCheck')
        .then(response => response.json())
        .then(data => {
            if (!data.loggedIn) {
                // Admin not logged in, redirect to login page
                window.location.href = 'admin-login.html';
            }
        })
        .catch(error => {
            console.error('Session check error:', error);
            // On error, redirect to login for security
            window.location.href = 'admin-login.html';
        });
}

// Run session check when page loads
document.addEventListener('DOMContentLoaded', function() {
    checkAdminSession();
});

//Periodically check session (every 5 minutes)
setInterval(checkAdminSession, 5 * 60 * 1000);