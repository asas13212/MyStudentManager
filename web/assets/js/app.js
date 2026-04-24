(function () {
  var mock = window.StudentManageMock || { students: [] };

  function bindNav() {
    var buttons = document.querySelectorAll(".nav-item");
    var panels = document.querySelectorAll(".panel");

    buttons.forEach(function (button) {
      button.addEventListener("click", function () {
        var targetId = button.getAttribute("data-target");

        buttons.forEach(function (b) {
          b.classList.remove("is-active");
        });
        button.classList.add("is-active");

        panels.forEach(function (panel) {
          panel.classList.remove("is-visible");
        });

        var target = document.getElementById(targetId);
        if (target) {
          target.classList.add("is-visible");
        }
      });
    });
  }

  function renderStats(students) {
    var ugCount = students.filter(function (s) {
      return s.type === "本科生";
    }).length;
    var pgCount = students.filter(function (s) {
      return s.type === "研究生";
    }).length;

    var ugNode = document.getElementById("ugCount");
    var pgNode = document.getElementById("pgCount");
    var totalNode = document.getElementById("totalCount");

    if (ugNode) ugNode.textContent = String(ugCount);
    if (pgNode) pgNode.textContent = String(pgCount);
    if (totalNode) totalNode.textContent = String(students.length);
  }

  function renderTable(students) {
    var tbody = document.getElementById("studentTableBody");
    if (!tbody) {
      return;
    }

    tbody.innerHTML = students
      .map(function (student) {
        return (
          "<tr>" +
          "<td>" + student.id + "</td>" +
          "<td>" + student.name + "</td>" +
          "<td>" + student.type + "</td>" +
          "<td>" + student.major + "</td>" +
          "</tr>"
        );
      })
      .join("");
  }

  bindNav();
  renderStats(mock.students);
  renderTable(mock.students);
})();

