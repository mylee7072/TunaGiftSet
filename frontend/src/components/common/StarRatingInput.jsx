// A row of clickable/keyboard-focusable star buttons rather than a <select> —
// same underlying 1~5 integer value, just a more direct interaction for a rating.
export function StarRatingInput({ id, value, onChange }) {
  const rating = Number(value) || 0;

  return (
    <div className="star-rating-input" role="radiogroup" aria-label="평점" id={id}>
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          role="radio"
          aria-checked={rating === star}
          aria-label={`${star}점`}
          className={`star-rating-input__star${star <= rating ? " is-filled" : ""}`}
          onClick={() => onChange(star)}
        >
          ★
        </button>
      ))}
      <span className="star-rating-input__value">{rating}점</span>
    </div>
  );
}
