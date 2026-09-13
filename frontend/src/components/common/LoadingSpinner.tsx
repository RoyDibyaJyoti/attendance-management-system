import React from 'react';
import { Loader2 } from 'lucide-react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  label?: string;
  className?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({
  size = 'md',
  label,
  className = '',
}) => {
  const sizeClasses = {
    sm: 'w-4 h-4',
    md: 'w-6 h-6',
    lg: 'w-10 h-10',
  };

  return (
    <div className={`flex flex-col items-center justify-center p-6 text-slate-500 gap-2.5 ${className}`}>
      <Loader2 className={`${sizeClasses[size]} animate-spin text-indigo-600`} />
      {label && <span className="text-sm font-medium text-slate-600 animate-pulse">{label}</span>}
    </div>
  );
};
