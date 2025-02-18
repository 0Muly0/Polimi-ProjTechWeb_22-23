/**
 * 
 */

{
	let menu,
		lastViewedList, resultsList, prodDetAdv, ordersList, cartList,
		pageManager = new PageManager();

	//When page is loaded 
	window.addEventListener("load", () => {
		if (sessionStorage.getItem("username") == null) {
			//User login not valid -> go to index.html
			window.location.href = "index.html";
		} else {
			//Initialize components and display initial content
			pageManager.start();
		}

	}, false);

	function Menu(_alert, searchForm) {
		this.alert = _alert;
		this.searchF = searchForm;

		this.registerEvent = function(manager) {

			//Home event listener
			document.querySelector("a[href='Home']").addEventListener('click', (e) => {
				e.preventDefault();
				//Load homepage
				manager.load("home");
			})
			//Orders event listener
			document.querySelector("a[href='Orders']").addEventListener('click', (e) => {
				e.preventDefault();
				//Load orders
				manager.load("orders");
			})
			//Cart event listener
			document.querySelector("a[href='Cart']").addEventListener('click', (e) => {
				e.preventDefault();
				//Load cart
				manager.load("cart");
			})
			//Logout event listener
			document.querySelector("a[href='Logout']").addEventListener('click', (e) => {
				//Invalidate session
				window.sessionStorage.removeItem('username');
			})

			//Search event listener
			this.searchF.querySelector("input[type='button']").addEventListener('click', (e) => {
				var self = this;
				var sForm = e.target.closest("form");

				if (sForm.checkValidity()) {
					//Removes possible precedent alerts
					self.alert.style.display = "none";

					//Valid search -> get keyword from input text
					var keyw = sForm.querySelector("input[type = 'text']").value;
					//Go to results
					pageManager.load("results", null, keyw);
					
					//Clear form
					sForm.reset();
				} else {
					//Invalid search
					self.alert.textContent = "Invalid search";
					self.alert.style.display = "block";
					sForm.reportValidity();
				}
			})
		}
	}

	//Page manager class -> manages the view components
	function PageManager() {

		var self = this;
		var alertContainer = document.getElementById("id_alert");
		var mainBody = {
			home: document.getElementById("id_home"),
			results: document.getElementById("id_results"),
			orders: document.getElementById("id_orders"),
			cart: document.getElementById("id_cart"),
		}

		this.start = function() {
			//Hides all sections
			self.show("none");

			//Menu instance
			menu = new Menu(
				document.getElementById("id_menu_alert"),
				document.getElementById("id_searchform")
			);
			//Menu EventListeners initialization. Page manager passes itself so when an event is fired from the menu the page can be refreshed
			menu.registerEvent(pageManager);


			//Home -> Last Viewed List instance
			lastViewedList = new LastViewedList(
				pageManager,
				alertContainer,
				document.getElementById("id_lastviewedbody")
			);

			//Results
			resultsList = new ResultList(
				pageManager,
				alertContainer,
				document.getElementById("id_results_table"),
				document.getElementById("id_results_alert"),
				document.getElementById("id_results_body")
			)

			//Product advertisements
			prodDetAdv = new ProdDetailsAdvertisements(
				pageManager,
				alertContainer,
				{
					container: document.getElementById("id_proddetails"),
					title: document.getElementById("id_det_title"),
					photo: document.getElementById("id_det_photo"),
					name: document.getElementById("id_det_name"),
					description: document.getElementById("id_det_description"),
					category: document.getElementById("id_det_category"),
				},
				{
					container: document.getElementById("id_prodadvertisement"),
					title: document.getElementById("id_adv_title"),
					body: document.getElementById("id_adv_body")
				}
			);

			//Cart
			cartList = new CartList(
				pageManager,
				alertContainer,
				document.getElementById("id_cart_body"),
				document.getElementById("id_cart_error")
			);
			//Initialize cart session to null
			window.sessionStorage.setItem("cart", null);

			//Orders
			ordersList = new OrdersList(
				pageManager,
				alertContainer,
				document.getElementById("id_orders_body")
			);

			//Load home as firts page
			self.load("home");
		}

		this.load = function(page, _codeProd, _keyword) {
			alertContainer.innerHTML = '';

			switch (page) {
				case "home":
					//Load homepage
					lastViewedList.loadContent();
					break;
				case "results":

					if (_codeProd != null && _codeProd != '') {
						//Rendering details and advertisements	
						prodDetAdv.loadContent(_codeProd);
						
						//Render results
						resultsList.loadContent(null, _codeProd);
					} else {
						//Hiding details and advertisements
						prodDetAdv.hideContent();

						//Render results
						resultsList.loadContent(_keyword, null);
					}

					break;
				case "advertisements":
					//Rendering details and advertisements	
					prodDetAdv.loadContent(_codeProd);
					break;
				case "orders":
					//Load orders
					ordersList.loadContent();
					break;
				case "cart":
					//Load cart
					cartList.loadContent();
					break;
			}

		}

		this.show = function(page) {
			alertContainer.innerHTML = '';

			switch (page) {
				case "home":
					mainBody.home.style.display = "block";
					mainBody.results.style.display = "none";
					resultsList.resetContent();
					prodDetAdv.resetContent();
					mainBody.orders.style.display = "none";
					ordersList.resetContent();
					mainBody.cart.style.display = "none";
					cartList.resetContent();
					break;
				case "results":
					mainBody.home.style.display = "none";
					lastViewedList.resetContent();
					mainBody.results.style.display = "block";
					mainBody.orders.style.display = "none";
					ordersList.resetContent();
					mainBody.cart.style.display = "none";
					cartList.resetContent();
					break;
				case "orders":
					mainBody.home.style.display = "none";
					lastViewedList.resetContent();
					mainBody.results.style.display = "none";
					resultsList.resetContent();
					prodDetAdv.resetContent();
					mainBody.orders.style.display = "block";
					mainBody.cart.style.display = "none";
					cartList.resetContent();
					break;
				case "cart":
					mainBody.home.style.display = "none";
					lastViewedList.resetContent();
					mainBody.results.style.display = "none";
					resultsList.resetContent();
					prodDetAdv.resetContent();
					mainBody.orders.style.display = "none";
					ordersList.resetContent();
					mainBody.cart.style.display = "block";
					break;
				case "none":
					mainBody.home.style.display = "none";
					mainBody.results.style.display = "none";
					mainBody.orders.style.display = "none";
					mainBody.cart.style.display = "none";
					break;
			}
		}
	}


	function LastViewedList(manager, _alert, _listbody) {
		this.pageManager = manager;
		this.alert = _alert
		this.listBody = _listbody

		this.loadContent = function() {
			//Save itself to call render content function
			var self = this;
			//Removes old content
			self.resetContent();

			//Load last visualized
			makeCall('GET', 'GetLastVisualized', null,
				function(req) {
					if (req.readyState == 4) {
						//DONE

						//Save possible error message
						var message = req.responseText;
						if (req.status == 200) {
							//OK
							var lastViewedProds = JSON.parse(req.responseText);
							self.renderContent(lastViewedProds);
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

		this.renderContent = function(lastViewedProds) {
			var self = this;

			//Populates last viewed product table
			lastViewedProds.forEach(
				function(product) {
					var prodRow = document.createElement("tr");

					//Photo
					var cellPhoto = document.createElement("td");
					var photo = document.createElement("img");
					photo.src = "data:image/png;base64," + product.photo;
					photo.width = 100;
					cellPhoto.appendChild(photo);
					prodRow.appendChild(cellPhoto);

					//Name anchor
					var cellName = document.createElement("td");
					var name = document.createElement("a");
					name.textContent = product.name;
					name.href = product.name;
					name.addEventListener('click', (e) => {
						e.preventDefault();
						pageManager.load("results", product.codeProduct);
					}, false);
					cellName.appendChild(name);
					prodRow.appendChild(cellName);

					//Description
					var cellDescription = document.createElement("td");
					cellDescription.textContent = product.description;
					prodRow.appendChild(cellDescription);

					//Category
					var cellCategory = document.createElement("td");
					cellCategory.textContent = product.category;
					prodRow.appendChild(cellCategory);

					//Adds row to table
					self.listBody.appendChild(prodRow);
				}
			);

			//Shows home section
			pageManager.show("home");
		}

		this.resetContent = function() {
			var self = this;
			self.listBody.innerHTML = '';
		}
	}

	function ResultList(manager, _alert, _listtable, _listalert, _listbody) {
		this.pageManager = manager;
		this.alert = _alert
		this.listTable = _listtable
		this.listAlert = _listalert
		this.listBody = _listbody

		this.loadContent = function(keyword, codeProd) {
			//Save itself to call render content function
			var self = this;
			//Removes old content
			self.resetContent();

			//Building url
			var url = "GetSearchResults?";
			codeProd ? url += ("codeProduct=" + codeProd + "&") : null;
			keyword ? url += ("keyword=" + keyword) : null;
			//Removing &
			if (url.endsWith('&')) {
				url = url.substring(0, url.length - 1)
			}

			//Load search results
			makeCall('GET', url, null,
				function(req) {
					if (req.readyState == 4) {
						//DONE

						//Save possible error message
						var message = req.responseText;
						if (req.status == 200) {
							//OK
							var resultsProds = JSON.parse(req.responseText);
							self.renderContent(resultsProds);
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

		this.renderContent = function(resultsProds) {
			var self = this;
			
			if(resultsProds.length > 0) {							
				resultsProds.forEach(
					function(product) {
						var prodRow = document.createElement("tr");
	
						//Code anchor
						var cellCode = document.createElement("td");
						var code = document.createElement("a");
						code.textContent = product.codeProduct;
						code.href = product.codeProduct;
						code.addEventListener('click', (e) => {
							e.preventDefault();
							pageManager.load("advertisements", product.codeProduct);
						}, false);
						cellCode.appendChild(code);
						prodRow.appendChild(cellCode);
	
						//Name
						var cellName = document.createElement("td");
						cellName.textContent = product.name;
						prodRow.appendChild(cellName);
	
						//Minprice
						var cellMinprice = document.createElement("td");
						cellMinprice.textContent = '$' + product.minprice;
						prodRow.appendChild(cellMinprice);
	
						//Adds row to table
						self.listBody.appendChild(prodRow);
					}
				);
				self.listTable.style.display = "block";
				self.listAlert.style.display = "none";	
			} else {
				self.listTable.style.display = "none";
				self.listAlert.style.display = "block";
			}

			//Shows results section
			pageManager.show("results");
		}

		this.resetContent = function() {
			var self = this;
			self.listBody.innerHTML = '';
		}
	}

	function ProdDetailsAdvertisements(manager, _alert, det, adv) {
		this.pageManager = manager;
		this.alert = _alert;
		this.pdetails = det;
		this.padv = adv;

		this.loadContent = function(codeProd) {
			var self = this;
			self.resetContent();

			var cartJSON = {
				cart: JSON.parse(window.sessionStorage.getItem("cart"))
			}
			//Load product details and Advertisements
			makeJsonCall("POST", "GetProductAdvertisments?codeProduct=" + codeProd, cartJSON,
				function(req) {
					if (req.readyState == 4) {
						//DONE

						//Save possible error message
						var message = req.responseText;
						if (req.status == 200) {
							//OK
							var prodsDetAdv = JSON.parse(req.responseText);
							self.renderDetails(prodsDetAdv.details);
							self.renderAdvs(prodsDetAdv.details.name, prodsDetAdv.advs);
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

		this.renderDetails = function(prodDetails) {
			var self = this;

			self.pdetails.title.textContent = prodDetails.name + "'s details:";
			self.pdetails.photo.src = 'data:image/jpeg;base64,' + prodDetails.photo;
			self.pdetails.name.textContent = prodDetails.name;
			self.pdetails.description.textContent = prodDetails.description;
			self.pdetails.category.textContent = prodDetails.category;

			this.pdetails.container.style.display = "block";
		}

		this.renderAdvs = function(prodName, prodAdvertisements) {
			var self = this;

			self.padv.title.textContent = prodName + "'s advertisements:";

			prodAdvertisements.forEach(
				function(advertisement) {
					var advRow = document.createElement("tr");

					//Advertisement
					var advCell = document.createElement("td");
					var advBulletList = document.createElement("ul");

					//Supplier section
					var supplierContent = document.createElement("li");
					//Name
					var supplierName = document.createElement("h2");
					supplierName.textContent = advertisement.supplier.name;
					supplierContent.appendChild(supplierName);

					//Details:
					var supplierDetails = document.createElement("ul");

					//Rating
					var supplRating = document.createElement("li");
					supplRating.textContent = "Rating: " + advertisement.supplier.evaluation + "/5";
					supplierDetails.appendChild(supplRating);

					//Shipping policy:
					var shippingContainer = document.createElement("li");

					var shippingTitle = document.createElement("div");
					shippingTitle.textContent = "Shipping Price Ranges:";
					shippingContainer.appendChild(shippingTitle);

					var rangeList = document.createElement("ul");
					var rangeIdx = 1;

					advertisement.supplier.shippingPolicy.forEach(
						function(shippingRange) {
							var range = document.createElement("li");

							var rangeTitle = document.createElement("div");
							rangeTitle.textContent = "Range " + rangeIdx;
							range.appendChild(rangeTitle);

							var rangeDetails = document.createElement("ul");

							//Min
							var rangeMin = document.createElement("li");
							rangeMin.textContent = 'Minimum amount of products: ' + shippingRange.min;
							rangeDetails.appendChild(rangeMin);
							//Max
							var rangeMax = document.createElement("li");
							rangeMax.textContent = 'Maximum amount of products: ' + shippingRange.max;
							rangeDetails.appendChild(rangeMax);
							//Price
							var rangePrice = document.createElement("li");
							rangePrice.textContent = 'Shipping price: $' + shippingRange.shippingPrice;
							rangeDetails.appendChild(rangePrice);

							range.appendChild(rangeDetails);
							rangeList.appendChild(range);

							rangeIdx++;
						}
					)
					shippingContainer.appendChild(rangeList);
					supplierDetails.appendChild(shippingContainer);

					//UnitPrice
					var unitPrice = document.createElement("li");
					unitPrice.textContent = "Unit Price: " + advertisement.price;
					supplierDetails.appendChild(unitPrice);

					//Stock quantity
					var stockQty = document.createElement("li");
					if (advertisement.quantity > 0) {
						stockQty.textContent = "Available products: " + advertisement.quantity;
					} else {
						stockQty.textContent = "OUT OF STOCK";
					}
					supplierDetails.appendChild(stockQty);

					//Cart quantity
					var cartQty = document.createElement("li");
					cartQty.textContent = "Already in cart from this supplier: " + advertisement.cartQty;

					if (advertisement.cartQty > 0) {
						//Opens pop up
						cartQty.addEventListener('mouseenter', () => {
							window.open("http://localhost:8081/TIW-LuciaFamosoProjectJS/HoverCS.html?codeSupplier=" + advertisement.supplier.codeSupplier, "Already in cart from " + advertisement.supplier.name, "height=600,width=600, top=0, left=960");
						})
					}
					supplierDetails.appendChild(cartQty);

					//Cart total
					var cartTotal = document.createElement("li");
					cartTotal.textContent = "Cart total from this supplier: $" + advertisement.cartTot + ' (whitout shipping fee)';
					supplierDetails.appendChild(cartTotal);

					//Cart Form
					var cartFormCell = document.createElement("td");
					var cartForm = document.createElement("form");
					cartForm.setAttribute("action", "#");

					var prod = document.createElement("input");
					prod.setAttribute("name", "codeProduct");
					prod.setAttribute("type", "hidden");
					prod.setAttribute("value", advertisement.codeProduct);
					cartForm.appendChild(prod);

					var suppl = document.createElement("input");
					suppl.setAttribute("name", "codeSupplier");
					suppl.setAttribute("type", "hidden");
					suppl.setAttribute("value", advertisement.supplier.codeSupplier);
					cartForm.appendChild(suppl);

					var quantity = document.createElement("input");
					quantity.required = true;
					quantity.setAttribute("name", "cartqty");
					quantity.setAttribute("type", "number");
					quantity.setAttribute("min", 1);
					quantity.setAttribute("max", advertisement.quantity);
					quantity.setAttribute("placeholder", "quantity");
					cartForm.appendChild(quantity);

					cartForm.appendChild(document.createElement("br"));

					var submit = document.createElement("input");
					submit.setAttribute("type", "button");
					submit.setAttribute("name", "addToCart");
					submit.setAttribute("value", "addToCart");
					submit.addEventListener('click', (e) => {
						var cartForm = e.target.closest("form");
						var formAndCart = {
							form: {
								codeProduct: cartForm.querySelector('input[name="codeProduct"]').value,
								codeSupplier: cartForm.querySelector('input[name="codeSupplier"]').value,
								cartqty: cartForm.querySelector('input[name="cartqty"]').value,
							},
							cart: JSON.parse(window.sessionStorage.getItem("cart"))
						}

						if (cartForm.checkValidity()) {							
							makeJsonCall('POST', 'AddToCart', formAndCart,
								function(req) {
									if (req.readyState == 4) {
										//DONE

										//Save possible error message
										var message = req.responseText;
										if (req.status == 200) {
											//OK
											//Updated cart is returned as Json and saved in session storage
											window.sessionStorage.setItem("cart", req.responseText);
											//Load cart details
											pageManager.load("cart");
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
								});
						} else {
							cartForm.reportValidity();
						}
					})
					cartForm.appendChild(submit);

					//Attatching components			
					supplierContent.appendChild(supplierDetails);
					advBulletList.appendChild(supplierContent);
					advCell.appendChild(advBulletList);
					cartFormCell.appendChild(cartForm);

					advRow.appendChild(advCell);
					advRow.appendChild(cartFormCell);
					self.padv.body.appendChild(advRow);
				}
			)

			this.padv.container.style.display = "block";
		}

		this.hideContent = function() {
			var self = this;
			
			this.pdetails.container.style.display = "none";
			this.padv.container.style.display = "none";
			
			self.resetContent();
		}
		
		this.resetContent = function() {
			var self = this;
			
			self.pdetails.title.innerHTML = '';
			self.pdetails.photo.setAttribute("src", "");
			self.pdetails.name.innerHTML = '';
			self.pdetails.description.innerHTML = '';
			self.pdetails.category.innerHTML = '';
			
			self.padv.title.innerHTML = '';
			self.padv.body.innerHTML = '';
		}
	}

	function OrdersList(manager, _alert, ordersbody) {
		this.pageManager = manager;
		this.alert = _alert;
		this.ordersBody = ordersbody;

		this.loadContent = function() {
			var self = this;
			self.resetContent();

			makeCall("GET", "GetOrders", null,
				function(req) {
					if (req.readyState == 4) {
						//DONE

						//Save possible error message
						var message = req.responseText;
						if (req.status == 200) {
							//OK
							var orders = JSON.parse(req.responseText);
							self.renderContent(orders);
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
				}, false);
		}

		this.renderContent = function(ordersDetails) {
			var self = this;
			var orders = ordersDetails.orders;
			var user = ordersDetails.user;

			if (orders && orders.length > 0) {
				var orderList = document.createElement("ul");

				orders.forEach(
					function(order) {
						var orderDetails = document.createElement("li");

						//Order code
						var code = document.createElement("span");
						code.textContent = 'Order code: ' + order.codeOrder;

						//Supplier and Products details
						var supplProdDetails = document.createElement("ul");

						var supplName = document.createElement("li");
						supplName.textContent = 'Supplier name: ' + order.supplier.name;
						supplProdDetails.appendChild(supplName);

						var userAddress = document.createElement("li");
						userAddress.textContent = 'Shipping address: ' + user.address;
						supplProdDetails.appendChild(userAddress);

						var date = document.createElement("li");
						date.textContent = 'Date: ' + order.date;
						supplProdDetails.appendChild(date);

						var total = document.createElement("li");
						total.textContent = 'Total: $' + order.total;
						supplProdDetails.appendChild(total);

						//Product List
						var products = document.createElement("li");

						var title = document.createElement("span");
						title.textContent = "Product list:";
						products.appendChild(title);

						var prodList = document.createElement("ul");
						order.products.forEach(
							function(prod) {
								var prodDetails = document.createElement("li");

								//Name
								var name = document.createElement("span");
								name.textContent = prod.name;

								//Details list
								var details = document.createElement("ul");

								var description = document.createElement("li");
								description.textContent = 'Description: ' + prod.description;
								details.appendChild(description);

								var category = document.createElement("li");
								category.textContent = 'Category: ' + prod.category;
								details.appendChild(category);

								var cartQty = document.createElement("li");
								cartQty.textContent = 'Quantity ordered: ' + prod.cartQty;
								details.appendChild(cartQty);

								//Adding components
								prodDetails.appendChild(name);
								prodDetails.appendChild(details);

								prodList.appendChild(prodDetails);
							}
						);
						products.appendChild(prodList);
						supplProdDetails.appendChild(products);

						orderDetails.appendChild(code);
						orderDetails.appendChild(supplProdDetails);

						orderList.appendChild(orderDetails);
					}
				);
				self.ordersBody.appendChild(orderList);
			} else {
				var emptyOrders = document.createElement("div");
				emptyOrders.textContent = "No orders yet, start shopping to fill your existential void";

				self.ordersBody.appendChild(emptyOrders);
			}

			pageManager.show("orders");
		}

		this.resetContent = function() {
			var self = this;
			self.ordersBody.innerHTML = '';
		}
	}

	function CartList(manager, _alert, cartbody, carterror) {
		this.pageManager = manager;
		this.alert = _alert;
		this.cartBody = cartbody;
		this.cartError = carterror;

		//Loads details from session cart
		this.loadContent = function() {
			var self = this;
			self.resetContent();

			var cartJSON = {
				cart: JSON.parse(window.sessionStorage.getItem("cart"))
			}
			makeJsonCall("POST", "RenderCart", cartJSON,
				function(req) {
					if (req.readyState == 4) {
						//DONE

						//Save possible error message
						var message = req.responseText;
						if (req.status == 200) {
							//OK
							//Complete cart with details from database is returned to be rendered
							var cartDetails = JSON.parse(req.responseText);
							cartList.renderContent(cartDetails);
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

		this.renderContent = function(cartDetails) {
			var self = this;
			var cartElements = cartDetails.cart;

			if (cartDetails.quantityError) {
				self.cartError.style.display = "block";
				window.sessionStorage.setItem("cart", JSON.stringify(cartElements));
			} else {
				self.cartError.style.display = "none";
			}

			//Cart not empty
			if (cartElements && Object.keys(cartElements).length > 0) {
				var cartTable = document.createElement("table");

				var elementIdx = 0;
				cartElements.forEach(
					function(cartElement) {
						var cartRow = document.createElement("tr");

						//Supplier and product list
						var supplProdCell = document.createElement("td");
						var supplProdList = document.createElement("ul");

						var supplProd = document.createElement("li");

						//Supplier
						var suppl = document.createElement("span");
						suppl.textContent = cartElement.supplier.name;
						supplProd.appendChild(suppl);

						//Product list
						var prodList = document.createElement("ul");
						var productIdx = 0;
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

								//Remove Form
								var removeForm = document.createElement("form");
								removeForm.setAttribute("action", "#");

								var cartElementIndex = document.createElement("input");
								cartElementIndex.setAttribute("type", "hidden");
								cartElementIndex.setAttribute("name", "cartElementIdx");
								cartElementIndex.setAttribute("value", elementIdx);
								removeForm.appendChild(cartElementIndex);

								var productIndex = document.createElement("input");
								productIndex.setAttribute("type", "hidden");
								productIndex.setAttribute("name", "productIdx");
								productIndex.setAttribute("value", productIdx);
								removeForm.appendChild(productIndex);

								var removeProduct = document.createElement("input");
								removeProduct.setAttribute("type", "button");
								removeProduct.setAttribute("name", "removeFromCart");
								removeProduct.setAttribute("value", 'Remove ' + product.name + ' from cart');
								removeProduct.addEventListener('click', (e) => {
									var removeForm = e.target.closest("form");
									var formAndCart = {
										form: {
											cartElementIdx: removeForm.querySelector('input[name="cartElementIdx"]').value,
											productIdx: removeForm.querySelector('input[name="productIdx"]').value,
										},
										cart: JSON.parse(window.sessionStorage.getItem("cart"))
									}

									makeJsonCall('POST', 'RemoveFromCart', formAndCart,
										function(req) {
											if (req.readyState == 4) {
												//DONE

												//Save possible error message
												var message = req.responseText;
												if (req.status == 200) {
													//OK
													//Updated cart is returned as json and saved in session storage
													window.sessionStorage.setItem("cart", req.responseText);
													//Load cart details
													pageManager.load("cart");
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
										});
								});
								removeForm.appendChild(removeProduct);

								//Append elements
								prod.appendChild(prodDetails);
								prod.appendChild(removeForm);
								prodList.appendChild(prod);

								//Increment prodIndex
								productIdx++;
							}
						)
						supplProd.appendChild(prodList);
						supplProdList.appendChild(supplProd);

						//Product total price
						var prodTot = document.createElement("li");
						prodTot.textContent = 'Total product price: $' + cartElement.productPrice;
						supplProdList.appendChild(prodTot);

						//Shipping total price
						var shippingTot = document.createElement("li");
						shippingTot.textContent = 'Total shipping price: $' + cartElement.shippingPrice;
						supplProdList.appendChild(shippingTot);

						//Order Form
						var orderCell = document.createElement("td");

						var orderForm = document.createElement("form");
						orderForm.setAttribute("action", "#");

						var cartOrderIndex = document.createElement("input");
						cartOrderIndex.setAttribute("type", "hidden");
						cartOrderIndex.setAttribute("name", "cartElementIdx");
						cartOrderIndex.setAttribute("value", elementIdx);
						orderForm.appendChild(cartOrderIndex);

						var createOrder = document.createElement("input");
						createOrder.setAttribute("type", "button");
						createOrder.setAttribute("name", "createOrder");
						createOrder.setAttribute("value", "Order from " + cartElement.supplier.name);
						createOrder.addEventListener('click', (e) => {
							var cartForm = e.target.closest("form");
							var formAndCart = {
								form: {
									cartElementIdx: cartForm.querySelector('input[name="cartElementIdx"]').value,
								},
								cart: JSON.parse(window.sessionStorage.getItem("cart"))
							}

							makeJsonCall('POST', 'CreateOrder', formAndCart,
								function(req) {
									if (req.readyState == 4) {
										//DONE

										//Save possible error message
										var message = req.responseText;
										if (req.status == 200) {
											//OK
											var cartError = JSON.parse(req.responseText);

											//Updated cart is always returned as Json and saved in session storage
											window.sessionStorage.setItem("cart", JSON.stringify(cartError.cart));

											if (cartError.quantityError) {
												//Go to cart if a quantity error is found
												pageManager.load("cart");
											} else {
												//Go to orders if the order is created correctly
												pageManager.load("orders");
											}
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
								});
						});
						orderForm.appendChild(createOrder);

						//Append elements
						supplProdCell.appendChild(supplProdList);
						orderCell.appendChild(orderForm);
						cartRow.appendChild(supplProdCell);
						cartRow.appendChild(orderCell);
						cartTable.appendChild(cartRow);

						//Increment elementIndex
						elementIdx++;
					}
				);

				self.cartBody.appendChild(cartTable);
			} else {
				var emptyCart = document.createElement("div");
				emptyCart.textContent = "No products yet in your cart";

				self.cartBody.appendChild(emptyCart);
			}

			//Show cart
			pageManager.show("cart");
		}

		this.resetContent = function() {
			var self = this;
			self.cartBody.innerHTML = '';
		}
	}
}