var UFS = (function() {

  function init() {
    $('#totalRating').barrating({
      theme : 'css-stars',
      allowEmpty : true
    });

    var contentFlawDetailsField = $('#contentFlawDetailsField');
    $('#contentFlaw').on('click', function(event) {
      if (this.checked) {
        contentFlawDetailsField.show();
      } else {
        contentFlawDetailsField.hide();
      }
    });

    var technicalFlawDetailsField = $('#technicalFlawDetailsField');
    $('#technicalFlaw').on('click', function(event) {
      if (this.checked) {
        technicalFlawDetailsField.show();
      } else {
        technicalFlawDetailsField.hide();
      }
    });

    var miscFeedbackDetailsField = $('#miscFeedbackDetailsField');
    $('#miscFeedback').on('click', function(event) {
      if (this.checked) {
        miscFeedbackDetailsField.show();
      } else {
        miscFeedbackDetailsField.hide();
      }
    });
  }

  return {
    init : init
  };
})();
