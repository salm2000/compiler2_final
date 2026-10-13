app = Application()
@app.route("/")
def first():
    return "first"
@app.route("/")
def second():
    return "second"

