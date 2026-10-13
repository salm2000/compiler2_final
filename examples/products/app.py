app = Application()
products = [{"id": 1, "name": "Notebook", "price": 12.5}, {"id": 2, "name": "Pencil", "price": 1.25}]

def find_by_id(items, target_id):
    for item in items:
        if item["id"] == target_id:
            return item
    return None

@app.route("/", methods=["GET"])
def list_products():
    return render_template("index.html", items=products)

@app.route("/products/new", methods=["GET"])
def new_product():
    return render_template("add.html")

@app.route("/products", methods=["POST"])
def create_product():
    next_id = len(products) + 1
    name = request.form["name"]
    price = float(request.form["price"])
    products.append({"id": next_id, "name": name, "price": price})
    return redirect("/")

@app.route("/products/<int:id>", methods=["GET"])
def product_details(id):
    product = find_by_id(products, id)
    if product == None:
        return "Product not found"
    return render_template("details.html", product=product)

@app.route("/products/<int:id>/delete", methods=["POST"])
def remove_product(id):
    for product in products:
        if product["id"] == id:
            products.remove(product)
            break
    return redirect("/")

