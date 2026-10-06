from flask import Flask, request, jsonify

app = Flask(__name__)

def predict(cgpa, skills, internship):
    # Simple academic-project prediction logic.
    # You can later replace this with a real ML model.
    score = (cgpa / 10.0) * 60

    skill_list = [s.strip().lower() for s in skills.split(",") if s.strip()]
    useful = {"java", "python", "html", "sql", "communication", "excel"}
    score += min(len(set(skill_list) & useful) * 5, 25)

    if internship.lower() == "yes":
        score += 15

    score = max(0, min(100, round(score)))

    if score >= 80:
        role = "Software Developer / Java Developer"
    elif score >= 65:
        role = "Junior Developer / Data Analyst"
    else:
        role = "Trainee / Internship Recommended"

    return score, role

@app.post("/predict")
def prediction():
    data = request.get_json(silent=True) or {}

    try:
        cgpa = float(data.get("cgpa", 0))
    except ValueError:
        return jsonify({"error": "CGPA must be a number"}), 400

    if cgpa < 0 or cgpa > 10:
        return jsonify({"error": "CGPA must be between 0 and 10"}), 400

    skills = str(data.get("skills", ""))
    internship = str(data.get("internship", "No"))

    score, role = predict(cgpa, skills, internship)

    return jsonify({
        "placement_probability": score,
        "recommended_role": role
    })

if __name__ == "__main__":
    print("Python prediction service running at http://localhost:5000")
    app.run(host="127.0.0.1", port=5000, debug=False)
