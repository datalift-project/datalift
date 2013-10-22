function changeDiv(){

  document.getElementById('module-container').style.backgroundColor = "#808080";
}

function changeState(elem) 
{
  var navIds = new Array('endpoint', 'wiki', 'projets', 'admin');
  var classToChange = document.getElementById(elem);

  for (i = 0 ; i < navIds.length ; i++)
  {
    var navId = document.getElementById(navIds[i]);
    navId.className = classToChange.className.replace(/(?:^|\s)active(?!\S)/g, "");
  }
  classToChange.className = classToChange.className + " active";
}

function clickHandler(anchor) 
{
  var hasClass = anchor.getAttribute('class');

  alert('test');
  if (hasClass !== 'active') {
    anchor.setAttribute('class', 'active');
  }
}

