$(document).ready(function()
    {
      $(".pull_notes").click(function()
      {
        var block_pos = $("#notes").position();
        //console.log(block_pos);
        if (block_pos.left == 0)
          $("#notes").animate({left : "-595px"}, 1000, 'easeInOutBack');
        else 
          $("#notes").animate({left : "0px"}, 1000, 'easeInOutBack');

        return false;
      });
    });
