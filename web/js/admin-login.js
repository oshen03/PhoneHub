async function adminSignIn() {
    const email = document.getElementById("adminEmail").value;
    const password = document.getElementById("adminPassword").value;

    const credentials = { email, password };
    
    const response = await fetch("AdminSignIn", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(credentials)
    });

    if (response.ok) {
        const result = await response.json();
        if (result.status) {
            window.location = "admin.html";
        } else {
            document.getElementById("adminMessage").innerHTML = 
                result.message || "Login failed";
        }
    } else {
        document.getElementById("adminMessage").innerHTML = "Server error";
    }
}