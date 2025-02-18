/**
 * AJAX call management
 */

function makeCall(method, url, formElement, callback, reset = true) {

	//Inner variable visible by closures
	var req = new XMLHttpRequest(); 

	//Fires the callback function whenever the request readyState changes
	req.onreadystatechange = function() {
		callback(req)
	}; // Closure -> sees inner variable req
	
	//Configure request
	req.open(method, url);
	
	if (formElement == null) {
		//Request sent without parameters
		req.send();
	} else {
		//Request sent with form datas
		req.send(new FormData(formElement));
	}
	
	if (formElement !== null && reset === true) {
		//Reset form
		formElement.reset();
	}
}

function makeJsonCall(method, url, payload, callback) {
	//Inner variable visible by closures
	var req = new XMLHttpRequest(); 

	//Fires the callback function whenever the request readyState changes
	req.onreadystatechange = function() {
		callback(req)
	}; // Closure -> sees inner variable req
	
	//Configure request
	req.open(method, url);
	
	if(payload) {
		req.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
		req.send(JSON.stringify(payload));
	}
}