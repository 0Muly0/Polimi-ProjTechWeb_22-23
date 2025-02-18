/**
 * Login management
 */

(function() { // avoid variables ending up in the global scope

	document.getElementById("loginbutton").addEventListener('click', (e) => {
		var form = e.target.closest("form");
		
		if (form.checkValidity()) {
			
			//Calls CheckLogin servlet
			makeCall("POST", 'CheckLogin', e.target.closest("form"),
				function(x) {
					if (x.readyState == XMLHttpRequest.DONE) {
						var message = x.responseText;
						switch (x.status) {
							case 200: // OK
								sessionStorage.setItem('username', message);
								window.location.href = "HomeCS.html";
								break;
							case 400: // Bad request
								document.getElementById("errormessage").textContent = message;
								break;
							case 401: // Unauthorized
								document.getElementById("errormessage").textContent = message;
								break;
							case 500: // Server error
								document.getElementById("errormessage").textContent = message;
								break;
						}
					}
				}
			);			
		} else {
			//Cancel invalid form fields
			form.reportValidity();
		}
	
	});

})();