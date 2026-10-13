app = Application()
chores = [{"id": 1, "label": "Read compiler notes", "done": False}]

@app.route("/", methods=["GET"])
def dashboard():
    return render_template("tasks.html", entries=chores)

@app.route("/tasks", methods=["POST"])
def add_chore():
    label = request.form["label"]
    chores.append({"id": len(chores) + 1, "label": label, "done": False})
    return redirect("/")

