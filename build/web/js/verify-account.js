async function verifyAccount() {

    const verificationCode = document.getElementById("vcode").value;

//    console.log(verificationCode);

    const verification = {

        verificationCode: verificationCode
    };

    const verificationJson = JSON.stringify(verification);

    const response = await fetch(
            "VerifyAccount",
            {
                method: "POST",
                body: verificationJson,
                header: {
                    "Content-Type": "application/json"
                }
            }
    );

    if (response.ok) {

        const responseJSON = await response.json();

        if (responseJSON.status) {//true
            window.location = "index.html";

        } else {

            if (responseJSON.message === "1") {//Email not found
                window.location = "login-register.html";

            } else {

                document.getElementById("message").innerHTML = responseJSON.message;
            }

        }
    } else {

        document.getElementById("message").innerHTML = "Please Try Again";

    }

}

