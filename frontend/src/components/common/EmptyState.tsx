import React from 'react';
import { Inbox } from 'lucide-react';

interface EmptyStateProps {
  title: string;
  description: string;
  icon?: React.ReactNode;
  action?: React.ReactNode;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  icon = <Inbox className="w-10 h-10 text-slate-300" />,
  action,
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-12 text-center">
      <div className="w-16 h-16 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-center mb-4">
        {icon}
      </div>
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      <p className="text-sm text-slate-500 max-w-sm mt-1 mb-5">{description}</p>
      {action && <div>{action}</div>}
    </div>
  );
};
