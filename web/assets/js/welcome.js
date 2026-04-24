(function () {
  var enterButton = document.getElementById("enterSystemButton");
  if (!enterButton) {
    return;
  }

  enterButton.addEventListener("click", function () {
    window.location.href = "./pages/app.html";
  });
})();

