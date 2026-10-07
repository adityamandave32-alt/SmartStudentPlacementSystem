from flask import Flask, request, jsonify

app = Flask(__name__)


def predict(cgpa, skills, internship):

    score = 0

    # CGPA
    if cgpa >= 8:
        score += 50
    elif cgpa >= 7:
        score += 40
    elif cgpa >= 6:
        score += 30
    else:
        score += 20

    # Skills
    skill_count = len(skills.split(","))

    if skill_count >= 3:
        score += 30
    elif skill_count >= 2:
        score += 20
    else:
        score += 10

    # Internship
    if internship.lower() == "yes":
        score += 20

    if score >= 75:
        result = "High Placement Probability"
    elif score >= 55:
        result = "Medium Placement Probability"
    else:
        result = "Low Placement Probability"

    return score, result


@app.route("/predict", methods=["POST"])
def prediction():

    data = request.get_json()

    cgpa = float(data.get("cgpa", 0))
    skills = data.get("skills", "")
    internship = data.get("internship", "No")

    score, result = predict(
        cgpa,
        skills,
        internship
    )

    return jsonify({
        "score": score,
        "prediction": result
    })


if __name__ == "__main__":

    print("Python Prediction Server Started")
    print("Running on http://127.0.0.1:5000")

    app.run(
        host="127.0.0.1",
        port=5000,
        debug=True
    )