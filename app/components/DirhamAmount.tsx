type DirhamAmountProps = {
  amount: string;
  light?: boolean;
  className?: string;
};

/**
 * Renders a price with the official UAE Dirham glyph in place of "AED"/
 * "dirhams" wording, mirroring the Android app's DirhamAmount composable.
 */
export function DirhamAmount({ amount, light = false, className }: DirhamAmountProps) {
  return (
    <span className={`gc-dirham${light ? " is-light" : ""}${className ? ` ${className}` : ""}`}>
      <img src="/brand/currency-dirham.png" alt="AED" />
      {amount}
    </span>
  );
}

export function formatDirhamAmount(cents: number): string {
  return (cents / 100).toLocaleString("en-AE", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
