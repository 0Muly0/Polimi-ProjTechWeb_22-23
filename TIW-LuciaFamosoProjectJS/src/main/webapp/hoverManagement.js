/**
 * 
 */

{
	let alreadyInCartBox,
		pageManager = new PageManager();

	//When page is loaded 
	window.addEventListener("load", () => {
		pageManager.start();
	}, false);

	function PageManager() {
		var alertContainer = document.getElementById("id_alert");
		
		this.start = function() {

			//Already in cart box
			alreadyInCartBox = new AlreadyInCartBox(
				alertContainer,
				document.getElementById("id_box_container"),
				document.getElementById("id_box_title"),
				document.getElementById("id_box_error"),
				document.getElementById("id_box_body")
			);
			alreadyInCartBox.hide();

			//Retrieving code supplier from url
			var url = new URL(window.location.href);
			var params = new URLSearchParams(url.search);
			alreadyInCartBox.loadContent(params.get('codeSupplier'));
		}
	}

	function AlreadyInCartBox(alert, boxcontainer, boxtitle, boxerror, boxbody) {
		this.alert = alert;
		this.boxContainer = boxcontainer;
		this.boxTitle = boxtitle;
		this.boxError = boxerror;
		this.boxBody = boxbody;

		this.loadContent = function(codeSuppl) {
			var self = this;
			self.resetContent();

			var cartJSON = {
				cart: JSON.parse(window.sessionStorage.getItem("cart"))
			}

			makeJsonCall("POST", "RenderCart?codeSupplier=" + codeSuppl, cartJSON,
				function(req) {
					if (req.readyState == 4) {
						//DONE

						//Save possible error message
						var message = req.responseText;
						if (req.status == 200) {
							//OK
							//Complete cart with details from database is returned to be rendered
							var elementDetails = JSON.parse(req.responseText);
							self.renderContent(elementDetails);
						} else if (req.status == 403) {
							//Unauthorized
							window.location.href = req.getResponseHeader("Location");
							window.sessionStorage.removeItem('username');
						}
						else {
							//Other errors
							self.alert.textContent = message;
						}
					}
				}
			);
		}

		this.renderContent = function(supplierElement) {
			var self = this;
			var cartElement = supplierElement.cart[0];

			if (supplierElement.quantityError) {
				self.boxError.style.display = "block";
			} else {
				self.boxError.style.display = "none";
			}
			
			self.boxTitle.textContent = "Already in cart from " + cartElement.supplier.name + ":";

			//Product list
			var prodList = document.createElement("ul");
			cartElement.productsDet.forEach(
				function(product) {
					var prod = document.createElement("li");

					var prodName = document.createElement("span");
					prodName.textContent = product.name + ":";
					prod.appendChild(prodName);

					//Details
					var prodDetails = document.createElement("ul");

					var description = document.createElement("li");
					description.textContent = 'Description: ' + product.description;
					prodDetails.appendChild(description);

					var category = document.createElement("li");
					category.textContent = 'Category: ' + product.category;
					prodDetails.appendChild(category);

					var cartQty = document.createElement("li");
					cartQty.textContent = 'Quantity in cart: ' + product.cartQty;
					prodDetails.appendChild(cartQty);

					var cartUnitPrice = document.createElement("li");
					cartUnitPrice.textContent = 'Unit price: $' + product.cartUnitPrice;
					prodDetails.appendChild(cartUnitPrice);

					//Append elements
					prod.appendChild(prodDetails);
					prodList.appendChild(prod);
				}
			)
			self.boxBody.appendChild(prodList);

			//Product total price
			var prodTot = document.createElement("div");
			prodTot.textContent = 'Total product price: $' + cartElement.productPrice;
			self.boxBody.appendChild(prodTot);

			//Shipping total price
			var shippingTot = document.createElement("div");
			shippingTot.textContent = 'Total shipping price: $' + cartElement.shippingPrice;
			self.boxBody.appendChild(shippingTot);

			self.show();
		}


		this.show = function() {
			var self = this;
			self.boxContainer.style.display = "block";
		}

		this.hide = function() {
			var self = this;
			self.boxContainer.style.display = "none";
		}

		this.resetContent = function() {
			var self = this;
			self.boxBody.innerHTML = '';
		}
	}

}