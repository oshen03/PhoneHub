/* 
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/JavaScript.js to edit this template
 */

// Login function
async function signIn() {

    const email = document.getElementById("loginEmail").value;
    const password = document.getElementById("loginPassword").value;

    //    console.log(email);

    const signIn = {

        email: email,
        password: password
    };

    const signInJson = JSON.stringify(signIn);

    const response = await fetch(
        "SignIn",
        {
            method: "POST",
            body: signInJson,
            header: {
                "Content-Type": "application/json"
            }
        }
    );

    if (response.ok) {

        const responseJSON = await response.json();

        if (responseJSON.status) {//true
            if (responseJSON.message === "1") {//Email not found
                window.location = "verify-account.html";
            } else {
                window.location = "index.html";
            }
        } else {
            document.getElementById("loginMessage").innerHTML = responseJSON.message;
        }
    } else {
        document.getElementById("loginMessage").innerHTML = "Please Try Again";
    }

}

// Registration function
async function signUp() {

  const user = { 
    firstName: document.getElementById("regFirstName").value,
    lastName:  document.getElementById("regLastName").value,
    email:     document.getElementById("regEmail").value,
    password:  document.getElementById("regPassword").value,
    confirmPassword: document.getElementById("regConfirmPassword").value
  };

  const response = await fetch("/PhoneHub/SignUp", {
    method:  "POST",
    headers: { "Content-Type": "application/json" },
    body:    JSON.stringify(user)
  });



    if (response.ok) {//success

        const responseJSON = await response.json();
        //        console.log(responseJSON);

        if (responseJSON.status) { //if true
            //redirect another page
            //            document.getElementById("message").className="text-success";
            //            document.getElementById("message").innerHTML = responseJSON.message;

            window.location = "verify-account.html";
        } else {

            //custom message
            document.getElementById("regMessage").innerHTML = responseJSON.message;
        }
    } else {

        document.getElementById("regMessage").innerHTML = "Please Try Again";

    }

}