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

function loadDetails(elem)
{
  var navIds = new Array('sources', 'description', 'ontologies');
  var classToChange = document.getElementById(elem);

  for (i = 0 ; i < navIds.length ; i++)
  {
    var navId = document.getElementById(navIds[i]);
    //console.log(navId);
    navId.className = classToChange.className.replace(/(?:^|\s)active(?!\S)/g, "");
  }
  classToChange.className = classToChange.className + " active";
  if (elem == "sources")
  {
    document.getElementById('projectContent').innerHTML = "Source of project";
  }
}

$( document ).ready(function( $ ) {
  $('#tab_details a').click(function (e) {
    e.preventDefault();
    $(this).tab('show');
  });
  
  var anchor = window.location.hash;
   if (anchor.length > 0)
    $('#tab_details a[href="' + anchor + '"]').tab('show');
});

$( document ).ready(function( $ ) {
  $('#firstStepBtn').click(function (e) {
    $('#first_step').css('display', 'none');
    $('#hidden2').css('display', '');
    $('#hidden3').delay(800).queue(function (next){
      $(this).css('display', ''); next(); });
    $('#alert-ban').delay(800).queue(function (next){
      $(this).css('display', ''); next(); });
    $('#hidden4').delay(800).queue(function (next){
      $(this).css('display', ''); next(); });
    $('#hidden5').delay(800).queue(function (next){
      $(this).css('display', ''); next(); });
    $('#sourceName').delay(800).queue(function (next) {
      $(this).text("Kiosque_ouvert-Paris.rdf"); next(); });
  });
});












