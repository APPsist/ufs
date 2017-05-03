var UFS = (function() {
  var processId;

  function init(selectedProcessId, initialRating) {
    processId = selectedProcessId;

    var starRatingOptions =  {
      theme : 'fontawesome-stars-o',
      allowEmpty : true,
      readonly : true
    };

    if (initialRating) {
      starRatingOptions.initialRating = initialRating;
    }

    $('#processSelect').on('change', function(event) {
      var selection = $(this).val();
      window.location = 'feedbackOverview?pid=' + selection;
    });

    $('#totalRating').barrating(starRatingOptions);

    $('ul.feedbackList > li').on('click', function(event) {
      var fid = $(this).data('id');
      console.log(this, fid);
      var path = 'feedbackDetails?fid=' + fid + '&pid=' + processId;
      window.location = path;
    });
  }

  function clear() {
    console.log('Delete feedback for process ' + processId + '.');
  }

  return {
    init : init,
    clear : clear
  };
})();
