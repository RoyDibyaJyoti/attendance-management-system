import React from 'react';

interface StatCardProps {
  title: string;
  value: React.ReactNode;
  subtitle?: string;
  icon?: React.ReactNode;
  trend?: {
    value: string;
    isPositive?: boolean;
  };
  color?: 'indigo' | 'emerald' | 'amber' | 'rose' | 'slate';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  trend,
  color = 'indigo',
}) => {
  const iconBgClasses = {
    indigo: 'bg-indigo-50 text-indigo-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    amber: 'bg-amber-50 text-amber-600',
    rose: 'bg-rose-50 text-rose-600',
    slate: 'bg-slate-100 text-slate-600',
  };

  return (
    <div className="bg-white rounded-xl border border-slate-200/80 p-5 shadow-xs flex items-center justify-between gap-4">
      <div>
        <p className="text-xs font-semibold uppercase tracking-wider text-slate-500">{title}</p>
        <div className="text-2xl font-bold text-slate-900 mt-1 tracking-tight">{value}</div>
        {subtitle && <p className="text-xs text-slate-500 mt-0.5">{subtitle}</p>}
        {trend && (
          <div className="flex items-center gap-1 mt-1 text-xs font-medium">
            <span className={trend.isPositive ? 'text-emerald-600' : 'text-rose-600'}>
              {trend.value}
            </span>
          </div>
        )}
      </div>
      {icon && (
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center shrink-0 ${iconBgClasses[color]}`}>
          {icon}
        </div>
      )}
    </div>
  );
};
