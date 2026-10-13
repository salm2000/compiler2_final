app = Application()
@app.route("/")
def home():
    return render_template("missing.html")

